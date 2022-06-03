(ns metabook.server
  (:require [clojure.string :as str]
            [metabase.util.files :as u.files]
            [nextjournal.clerk :as clerk]))

;; TODO: make this recursively load srcs
;; TODO: make a better noload mechanism
^::clerk/no-cache
(defn load-local-src
  []
  (let [srcs (u.files/files-seq (u.files/get-path "local/src/nocommit/metabook/src/metabook"))
        noload ["server"]
        noload-pred (fn [string]
                      (some true? ((fn [s] (map #(str/includes? s %) noload)) string)))]
    (doseq [file (remove noload-pred srcs)]
      (println "loading: " (.toString file))
      (load-file (.toString file)))))

(defn server-start! []
  (clerk/serve! {:browse true
                 :port 7891
                 :watch-paths ["local/src/nocommit/metabook/notebooks"]}))

(defn start!
  []
  (load-local-src)
  (server-start!))

#_(start!)
