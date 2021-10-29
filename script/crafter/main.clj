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

(defn get-config-dir
  ([]
   (fs/path
     (or
       (System/getenv "XDG_CONFIG_DIR")
       (fs/path (System/getenv "HOME") ".config"))
     "crafter"))
  ([target] (fs/path (get-config-dir) target)))

(defn get-cache-dir
  ([] (fs/path
        (or
          (System/getenv "XDG_CACHE_DIR")
          (fs/path (System/getenv "HOME") ".cache"))
        "crafter"))
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
  (let [target-dir (get-cache-dir (name target))]
    (when-not (fs/exists? target-dir)
    (fs/create-dir target-dir))
    (let [target-version (first (:results (aur-request! target)))]

      ))
  )

(def builders
  {:pacman "" #_(fn [target] (shell/sh "pacman" "-Sy" (name target)))})

(defn read-deps! []
  (fs/list-dir (get-config-dir "pkgs")))

(defn -main []
  (aur-request! :neovim-git))
