# Query Processor

This one will take me a bit to learn properly.

## Cam\'s Demo

This is a gist that Cam provided on 03/06/2022.
[Link](https://gist.githubusercontent.com/camsaul/32d5348d7e6904cbdf930e4f0dafb5b0/raw/7fab0b33f0b707b492b0e10e92fe6d9a9e61e76c/x.clj)

Just pasted here for now. Notes when I have time.

``` clojure
(ns metabase.x
  (:require [clojure.test :refer :all]
            dev.debug-qp
            [metabase.models.database :refer [Database]]
            [metabase.query-processor :as qp]
            [metabase.test :as mt]
            [metabase.query-processor.context :as qp.context]
            [metabase.query-processor.context.default :as context.default]
            [clojure.pprint :as pprint]
            [metabase.driver.sql.query-processor :as sql.qp]
            [metabase.driver :as driver]
            [metabase.util :as u]))

(defn a-query []
  (mt/mbql-query venues
    {:order-by [[:asc $id]]}))

;; an MBQL query
#_(a-query)

;; back to MBQL shorthand
(defn to-mbql-shorthand []
  (dev.debug-qp/to-mbql-shorthand (a-query)))

;; running a query
(defn run-a-query []
  (mt/rows
   (qp/process-query (a-query))))

;; native query
(deftest native-test []
  (mt/with-native-query-testing-context (a-query)
    (is (= 1 2))))

;; HoneySQL
#_(mt/set-ns-log-level! 'metabase.driver.sql.query-processor :debug)

(defn ->honeysql []
  (mt/with-everything-store
    (sql.qp/->honeysql :h2 [:field 203972 nil])))

(defn different-reducing-fn []
  (qp/process-query (a-query)
                    (fn [initial-metadata]
                      (println "METADATA =>")
                      (pprint/pprint initial-metadata)
                      (fn rf
                        ;; initial
                        ([]
                         0)
                        ;; final
                        ([row-count]
                         row-count)
                        ;; each row
                        ([row-count _row]
                         (inc row-count))))
                    (context.default/default-context)))

(defn process-query-debug []
  (dev.debug-qp/process-query-debug (a-query)))


(driver/register! ::my-driver, :parent :h2)

(defmethod driver/execute-reducible-query ::my-driver
  [_driver _query _context respond]
  (let [metadata {:cols [{:name "X"}
                         {:name "Y"}]}
        rows     [[1 2]
                  [3 4]]]
    (future
      (Thread/sleep 4000)
      (respond metadata rows))))

(defn different-driver []
  (mt/with-temp Database [{db-id :id} {:details (:details (mt/db))
                                       :engine ::my-driver}]
    (qp/process-query {:database db-id, :type :native, :native {:query "HELLO"}})))


```
