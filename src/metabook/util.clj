(ns metabook.util
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [metabase.cmd.endpoint-dox :as endpoint-dox]
            [metabase.util :as mu]
            [metabase.test :as mt]
            [nextjournal.clerk :as clerk]))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

;; todo: not exactly secure
(def credentials (load-edn "local/src/nocommit/metabook/notebooks/.secret.edn"))
(def myself (mt/client credentials :get 200 "/user/current"))

(defn- clean-specific-endpoint-data
  [{:keys [doc endpoint-str]}]
  (let [endpoint-str (last (re-matches #".*`([^\)]*)`" endpoint-str))
        endpoint-segments (remove #{""} (str/split endpoint-str #"/"))]
    {:endpoint-str endpoint-str
     :method ((comp keyword str/trim str/lower-case first) endpoint-segments)
     :segments (into [] (map-indexed #(if (str/starts-with? %2 ":")
                                        (keyword (str/replace-first %2 #":" ""))
                                        %2)
                                     (drop 2 endpoint-segments)))}))

(defn- clean-endpoint-data
  [s]
  (mapv clean-specific-endpoint-data s))

(defn map-endpoints
  []
  (let [endpoint-map (-> (#'endpoint-dox/map-endpoints)
                         (update-keys (comp #(str/replace % #" " "-") str/lower-case))
                         (update-vals clean-endpoint-data))]
    endpoint-map))

(def endpoint-data (map-endpoints))

(defn- find-endpoint-segments
  [[endpoint & _ :as segments]]
  (let [possible-segments (->> (get endpoint-data endpoint)
                               (map :segments)
                               (filter #(= (count segments) (count %)))
                               set)
        exact-match (first (filter #{segments} possible-segments))
        compare-segments (fn [possible-segments]
                           (mapv #(cond (= %1 %2) %1
                                        (keyword? %2) (str %2)
                                        :else :no-match) segments possible-segments))]
    (or exact-match
        (->> (map compare-segments possible-segments)
             (remove #(seq (filter #{:no-match} %)))
             first))))

(defn api-req
  [& args]
  (let [[method args] (mu/optional keyword? args :get)
        [url args] (mu/optional string? args)
        segments (into [] (remove #{""} (str/split url #"/")))]
    (with-meta (apply mt/client (concat [credentials method url] args))
      {:endpoint (first segments)
       :segments (find-endpoint-segments segments)
       :method method
       :args args})))
