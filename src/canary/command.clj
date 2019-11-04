(ns canary.command
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]
            [muuntaja.core :as muuntaja]
            [clj-http.client :as client]))


(defmulti handle first)


(defmethod handle :add-profile [[_ {:keys [user profile]}]]
 (let [item {:partition (str user ":profile")
              :sort 123456
              :profile profile}]
   (faraday/put-item db/config :canary item)
   {}))


(defmethod handle :verify-authorisation [[_ {:keys [code]}]]
  (let [request-body {"client_id" (System/getenv "AUTHORISATION_CLIENT_ID")
                      "client_secret" (System/getenv "AUTHORISATION_CLIENT_SECRET")
                      "code" code}
        response (client/post "https://github.com/login/oauth/access_token"
                              {:body (slurp (muuntaja/encode "application/json" request-body))
                               :content-type :json
                               :accept :json})
        user (client/get "https://api.github.com/user"
                         {:headers {"Authorization" ["token" (get-in response [:body "access_token"])]}})]
    ;; get user id
    ;; create session
    (println "************************************")
    (println user)
    (println "************************************")
    {:hi :there}))


(defmethod handle :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))
