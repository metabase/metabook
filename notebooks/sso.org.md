# sso

SSO -\> Single Sign On

Single Sign On is a general term referring to the ability for a user to
use a third party site or service to log in to Metabase. A popular
example is \'Sign in with Google\'.

At Metabase we provide administrators with the ability to set up Sign on
via:

-   Google Authentication
-   Lightweight directory access protocol (LDAP)
-   Auth Provider using Security assertion markup language (SAML)
-   Auth Provider using JSON Web Token (JWT)

The details for each of these implementations vary, but at a high level
what is happening is not too complex:

1.  A Metabase Server is set up with at least one method of SSO
2.  At login, every user is given the \'SSO\' option. If clicked, the
    user is re-directed to the identity provider\'s address (outside of
    Metabase).
3.  User performs their login at this other address.
4.  Assuming successful login, the user is re-directed back to Metabase.
    I think to the `/auth/sso` endpoint.
5.  Metabase receives the user\'s data, creating a new user in the
    app-db or updating the user\'s data (if it has changed) and starts
    the user\'s session. They\'re logged in and good to go at this
    point.

``` clojure
^{:nextjournal.clerk/visibility :hide-ns}
(ns metabook.sso
  (:require [clojure.set :as set]
            [clojure.java.shell :as sh :refer [sh]]
            [clojure.string :as str]
            [clojure.math.combinatorics :as combo]
            [forge.brep.curves :as brep.c]
            [forge.brep.surfaces :as brep.s]
            [forge.brep.mesh :as brep.m]
            [forge.model :as mdl]
            [forge.compile.scad :as scad]
            [metabase.test :as mt]
            [metabook.util :as mbu]
            [svg-clj.composites :refer [svg]]
            [svg-clj.elements :as el]
            [svg-clj.layout :as lo]
            [svg-clj.path :as path]
            [svg-clj.parametric :as p]
            [svg-clj.tools :as tools]
            [svg-clj.transforms :as tf]
            [svg-clj.utils :as svgu]
            [nextjournal.clerk :as clerk]))

```

## Setting up JWT

[Here](https://github.com/metabase/sso-examples) is a cool Metabase
repository that has a few SSO examples. In particular, the
clj-jwt-example project is very helpful. I\'ve cloned it locally and
have made changes to `handler.clj`. I\'ve added some users for testing,
and have hard coded the `shared-secret`, which you are given in the
*Metabase Admin Panel*.

Here\'s the relevant code, I\'ve copied `handler.clj` and have made some
changes:

    (ns clj-jwt-example.handler
      (:require [buddy.sign.jwt :as jwt]
                [compojure
                 [core :refer :all]
                 [route :as route]]
                [hiccup.core :refer [html]]
                [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
                [ring.util.response :as resp]))

    (def ^:private users
      "Mapping that is a mock of a username/password database"
      {"test@metabase.com" {:email      "test@metabase.com"
                            :first-name "Test"
                            :last-name  "User"
                            :password   "test1"}
       "jwt-adam"          {:email      "adam.jwt@org.com"
                            :first-name "adam-jwt"
                            :last-name  "jwt-authorized"
                            :password   "test1"}
       "mystery"           {:email      "mystery@org.com"
                            :first-name "MYSTERY"
                            :password   "test1"}})

    (defn- authenticate-user [username password]
      (when-let [found-user (get users username)]
        (when (= (:password found-user) password)
          found-user)))

    (def ^:private shared-secret
      "shared secret string with metabase"
      "f139a46214777b70fc7c5958ca55a7016f930369cb42647bfc8a5b6912557124")

    (def ^:private metabase-jwt-url
      "If not hard-coding the Metabase JWT URL, it's a good idea to validate the host to prevent a man-in-the-middle attack"
      "http://localhost:3000/auth/sso")

    (defn- redirect-user-with-jwt [{:keys [email first-name last-name]} return_to]
      (let [jwt (jwt/sign (merge
                            (when first-name {:first_name first-name})
                            (when first-name {:last_name last-name})
                            {:email      email
                             :exp        (+ (int (/ (System/currentTimeMillis) 1000)) (* 60 60 24 7))}) ; S M H D
                          shared-secret)]
        (resp/redirect (str metabase-jwt-url "?jwt=" jwt "&return_to=" return_to))))

    (defroutes app-routes
      (GET "/login" [return_to]
           (html
            [:html
             [:h1 "Login"]
             [:form {:method "post", :action "/login"}
              [:label "Username"]
              [:input {:type "text"
                       :name "username"}]
              [:label "Password"]
              [:input {:type "password"
                       :name "password"}]
              [:input {:type "hidden"
                       :name "return_to"
                       :value return_to}]
              ;; Needs to be included with the anti-forgery middleware that is included in the `site-defaults` below
              [:input {:type "hidden"
                       :name "__anti-forgery-token"
                       :value ring.middleware.anti-forgery/*anti-forgery-token*}]
              [:input {:type "submit"
                       :value "Submit"}]]]))
      (POST "/login" {:as req}
            (let [{:strs [username password return_to]} (:form-params req)]
              (if-let [user (authenticate-user username password)]
                (redirect-user-with-jwt user return_to)
                {:status 403
                 :body "Authentication failed"})))
      (route/not-found "Not Found"))

    (def app
      (wrap-defaults app-routes site-defaults))

Once the changes are made, you can start the server with `lein`.

`lein ring server-headless 3535`

Then, to see if logins work, you can set up JWT auth via the Metabase
Admin panel.
