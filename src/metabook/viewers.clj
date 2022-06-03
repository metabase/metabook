(ns metabook.viewers
  (:require [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [metabase.test :as mt]
            [metabook.util :as mbu]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer]))

(defn- api-dispatch
  [m]
  (let [{:keys [method segments]} (meta m)]
    (vec (concat [method] segments))))

(defmulti api-table-for-clerk api-dispatch)
(defmethod api-table-for-clerk :default
  [response]
  (cond
    (map? response) {:head (keys response)
                     :rows [(vals response)]}

    (seq? response) {:head (keys (first response))
                     :rows (map vals response)}))

(defmethod api-table-for-clerk [:get "user"]
  [response]
  (let [keep-keys [:email :first_name :last_name :common_name :id]
        data (map #(select-keys % keep-keys) (:data response))]
    {:head (keys (first data))
     :rows (map vals data)}))

(defmethod api-table-for-clerk [:get "user" ":id"]
  [response]
  (let [keep-keys [:email :first_name :last_name :common_name :id]
        data (select-keys response keep-keys)]
    {:head (keys data)
     :rows [(vals data)]}))

(defmethod api-table-for-clerk [:get "user" "current"]
  [response]
  (let [keep-keys [:email :first_name :last_name :common_name :id]
        data (select-keys response keep-keys)]
    (clerk.viewer/code
      {:head (keys data)
       :rows [(vals data)]})))
