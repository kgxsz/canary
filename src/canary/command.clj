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


(defmethod handle :authorise [[_ {:keys [code]}]]
  (let [request-body {:client_id (System/getenv "AUTHORISATION_CLIENT_ID")
                      :client_secret (System/getenv "AUTHORISATION_CLIENT_SECRET")
                      :code code}
        {:keys [body]} (client/post
                        "https://github.com/login/oauth/access_token"
                        {:body (slurp (muuntaja/encode "application/json" request-body))
                         :content-type :json
                         :accept :json
                         :cookie-policy :standard})
        access-token (:access_token (muuntaja/decode "application/json" body))
        {:keys [body]} (client/get
                        "https://api.github.com/user"
                        {:headers {:authorization (format "token %s" access-token)}})
        user (muuntaja/decode "application/json" body)
        item {:partition (str (:login user) ":user")
              :sort 1234567
              :user {:id (:id user)
                     :name (:login user)
                     :created-at 1234567}}]
    #_(faraday/put-item db/config :canary item)
    {:current-user-id (:id user)}))


(defmethod handle :deauthorise [command]
  {:current-user-id nil})


(defmethod handle :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))
