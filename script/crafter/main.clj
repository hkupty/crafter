(ns crafter.main
  (:require [babashka.fs :as fs]
            [babashka.curl :as curl]
            [clojure.string :as str]
            [cheshire.core :as json]))

;; TODO Break into two steps: compiling manifest and then building
;; TODO Dependency check before building
;; TODO Sanity checking manifest before applying build
;; TODO Allowing for other (external, i.e. pip) builders
;; TODO Rework package structure: Group is a folder, each file is a package.
;; TODO Within groups, allow for comments (like, why installing, etc...)
;; TODO Allow for post-install operations like applying .pacnew files
;; TODO Allow for post-install user-scripts, configured in .config/crafter/pkgs/<group>/<pkg>.end file
;; TODO Allow for dry-running
;; TODO CLI for managing pkgs folder
;; TODO cleanup old files
;; TODO allow for immutable fs and snapshot based install

(defn create-dir [& parts]
  (let [path (apply fs/path parts)]
    (when-not (fs/exists? path)
      (fs/create-dir path))))

(defn get-config-dir
  ([]
   (let [config-dir (fs/path
     (or
       (System/getenv "XDG_CONFIG_DIR")
       (fs/path (System/getenv "HOME") ".config"))
     "crafter")]
     (create-dir config-dir)
     config-dir))
  ([target] (fs/path (get-config-dir) target)))

(defn get-cache-dir
  ([] (let [cache-dir (fs/path
        (or
          (System/getenv "XDG_CACHE_DIR")
          (fs/path (System/getenv "HOME") ".cache"))
        "crafter")]
        (create-dir cache-dir)
        cache-dir))
  ([target] (fs/path (get-cache-dir) target)))

(defn aur-request! [target]
  (json/parse-string
    (:body
      (curl/get
        (str/join "&"
                  ["https://aur.archlinux.org/rpc/?v=5"
                   "type=info"
                   (str "arg[]=" (name target))])))
      true))


(defn aur-handler [target]
  (let [package-cache-dir (get-cache-dir (name target))]
    (create-dir package-cache-dir)
    (let [target-version (first (:results (aur-request! target)))
          target-path (fs/path package-cache-dir (-> target-version :Version))
          file-path (str (fs/path target-path (last (str/split (:URLPath target-version) #"/"))))]
      (println file-path)

      (create-dir target-path)
      (spit file-path
            (:body (curl/get (str "https://aur.archlinux.org" (:URLPath target-version))))))))

(defn read-deps! []
  (fs/list-dir (get-config-dir "pkgs")))

(defn -main []
  (aur-handler :neovim-git))
