(ns canary.command
  (:require [canary.db :as db]
            [canary.query :as query]
            [taoensso.faraday :as faraday]
            [muuntaja.core :as muuntaja]
            [clj-http.client :as client]
            [clj-time.core :as time]))


(defmulti handle first)


(defmethod handle :add-profile [[_ {:keys [user]}]]
  (let [now (time.coerce/to-long (time/now))
        item {:partition (str "profile:" (:id user))
              :data {:user-id (:id user)
                     :handle (:login user)
                     :email (:email user)
                     :name (:name user)
                     :avatar (:avatar_url user)
                     :location (:location user)
                     :created-at now
                     :last-authorised now}}]
    (faraday/put-item db/config :canary item)))


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
        {user-id :id} (muuntaja/decode "application/json" body)
        profile (query/query-profile user-id)]
    {:current-user-id (when (some? profile) user-id)}))


(defmethod handle :deauthorise [command]
  {:current-user-id nil})


(defmethod handle :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))
