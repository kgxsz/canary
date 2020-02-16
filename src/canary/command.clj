(ns canary.command
  (:require [canary.db :as db]
            [canary.query :as query]
            [taoensso.faraday :as faraday]
            [muuntaja.core :as muuntaja]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as t.format]
            [clj-time.coerce :as t.coerce]))


(defn create-profile [user]
  (let [now (t.coerce/to-long (t/now))]
    (-> user
        (select-keys [:id :login :email :name :avatar_url :location])
        (clojure.set/rename-keys {:id :user-id :login :handle :avatar_url :avatar})
        (assoc :created-at now :last-authorised now))))


(defn add-profile [{:keys [user-id] :as profile}]
  (let [item {:partition (str "profile:" user-id)
              :data profile}]
    (faraday/put-item db/config :canary item)))


(defn update-grids [user-id grids]
  (let [partition (str "grids:" user-id)]
    (faraday/put-item db/config :canary {:partition partition :data grids})))


(defmulti handle first)


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
        {user-id :id :as user} (muuntaja/decode "application/json" body)
        profile (query/query-profile user-id)]
    (when (some? profile) (-> user create-profile add-profile))
    {:current-user-id (when (some? profile) user-id)}))


(defmethod handle :deauthorise [command]
  {:current-user-id nil})


(defmethod handle :toggle-checked-date [[_ {:keys [i checked-date current-user-id]}]]
  (let [grids (query/query-grids current-user-id)
        path [i :checked-dates]
        checked-dates (get-in grids path)
        operation (if (contains? checked-dates checked-date) disj conj)]
    (when (some? current-user-id)
      (update-grids current-user-id (update-in grids path operation checked-date)))
    {}))


(defmethod handle :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))
