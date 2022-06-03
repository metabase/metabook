# Metabasics

``` clojure
^{:nextjournal.clerk/visibility :hide-ns}
(ns metabook.metabasics
  (:require [clojure.java.shell :as sh :refer [sh]]
            [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [metabase.test :as mt]
            [metabook.util :as mbu]
            [metabook.viewers :as mbv]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as clerk.viewer]))
```

Notes regarding Metabase and how it works, with a backend focus. Also,
this is a rough document at the moment, doesn\'t really have good
structure yet.

# Proof of Concept Viewer Multimethods for Viewing API responses

I think it\'d be quite cool to build some simple viewers (maybe even
with some state for basic UI stuff) for Metabase\'s entities.

I\'ll start with some tables for some API responses, and can build out
from there. NOTE: I want to put stuff in their own namespaces, but this
is a proof of concept as I figure things out.

The code for this is not all shown Clerk-side, but here are a few things
to improve:

-   the state setup is poor -\> I\'ve got a few UI elements (button and
    textbox) that each have their own state atom, maybe unify this?
-   it\'s a pain to write, and confusing to follow the code -\> perhaps
    a macro to generate these UI bits is a good idea. TBD

### API Response

``` clojure
^:nextjournal.clerk/no-cache
(let [response @api-response]
  (mbv/api-table-for-clerk response))

^{::clerk/visibility :hide
  ::clerk/viewer :table}
^:nextjournal.clerk/no-cache
(let [response @api-response]
  (mbv/api-table-for-clerk response))
```

# Build an API tool

A basic Metabase API tool. This let\'s you do GET requests. Type the
endpoint URL and press \'Make API Request\'. Fails silently at the
moment 🤷‍♂️.

``` clojure
^{::clerk/visibility :hide ::clerk/viewer clerk/hide-result}
(defonce api-response (atom {}))
```

``` clojure
^{::clerk/visibility :hide
  ::clerk/viewer
  {:pred #(when-let [v (get % ::clerk/var-from-def)]
            (and v (instance? clojure.lang.IDeref (deref v))))
   :fetch-fn (fn [_ x] x)
   :transform-fn (fn [{::clerk/keys [var-from-def]}]
                   {:var-name (symbol var-from-def) :value @@var-from-def})
   :render-fn '(fn [{:keys [var-name value]}]
                 (v/html [:input {:type :text
                                  :placeholder "API paths"
                                  :initial-value value
                                  :class "px-3 py-3 placeholder-blueGray-300 text-blueGray-600 relative bg-white bg-white rounded text-sm border border-blueGray-300 outline-none focus:outline-none focus:ring w-full"
                                  :on-input #(v/clerk-eval `(reset! ~var-name ~(.. % -target -value)))}]))}}
(defonce text-state (atom ""))

^{::clerk/visibility :hide
  ::clerk/viewer {:fetch-fn (fn [_ x] x)
                  :transform-fn (fn [{:as x ::clerk/keys [var-from-def]}]
                                  {:var-name (symbol var-from-def) :value @@var-from-def})
                  :render-fn '(fn [{:as x :keys [var-name value]}]
                                (v/html [:input {:type :button
                                                 :class "border border-green-500 bg-green-500 text-white rounded-md px-4 py-2 m-2 transition duration-500 ease select-none hover:bg-green-600 focus:outline-none focus:shadow-outline"
                                                 :initial-value value
                                                 :value "Make API Request"
                                                 :on-click #(v/clerk-eval `(reset! ~var-name true))}]))}}
(defonce button-state (atom false))

^{::clerk/visibility :hide ::clerk/viewer clerk/hide-result}
(add-watch button-state :button
           (fn [key atom old-state new-state]
             (when new-state
               (do
                 (reset! api-response (mbu/api-req :get @text-state))
                 (reset! button-state false)))))
```
