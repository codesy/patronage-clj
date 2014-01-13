(ns patronage.auth.github
  (:require [cemerick.friend        :as    friend]
            [compojure.core         :refer :all]
            [environ.core           :refer [env]]
            [friend-oauth2.workflow :as    oauth2]
            [friend-oauth2.util     :refer [format-config-uri
                                            get-access-token-from-params]]
            [ring.util.response     :as    response]
            [taoensso.timbre        :as    timbre])
  (:import  (java.net MalformedURLException
                      URL)))

(def config-auth {:roles #{::user}})

(defn github-oauth-client-id
  []
  (env :github-oauth-client-id))

(defn github-oauth-client-secret
  []
  (env :github-oauth-client-secret))

(defn github-oauth-callback
  []
  (if-let [url (URL. (env :github-oauth-callback))]
    {:domain (str (.getProtocol url) "://" (.getHost url) ":" (.getPort url))
     :path   (.getPath url)}))

(def client-config
  {:client-id     (get-oauth-client-id)
   :client-secret (get-oauth-client-secret)
   :callback      (get-oauth-callback)})

(def uri-config
  {:authentication-uri
   {:url   "https://github.com/login/oauth/authorize"
    :query {:client_id     (:client-id client-config)
            :response_type "code"
            :redirect_uri  (format-config-uri client-config)
            :scope         "user:email"}}

   :access-token-uri
   {:url   "https://github.com/login/oauth/access_token"
    :query {:client_id     (:client-id client-config)
            :client_secret (:client-secret client-config)
            :grant_type    "authorization_code"
            :redirect_uri  (format-config-uri client-config)}}})

(def github-workflow
  (oauth2/workflow
   {:client-config        client-config
    :uri-config           uri-config
    :access-token-parsefn get-access-token-from-params
    :config-auth          config-auth}))

(defroutes auth-routes
  (friend/logout
   (ANY "/logout" [request] (response/redirect "/"))))