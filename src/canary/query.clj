(ns canary.query
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]))


(defn query-profile [user-id]
  (let [query {:partition (str "profile:" user-id)}]
    (:data (faraday/get-item db/config :canary query))))


(defn query-grids [user-id]
  (let [query {:partition (str "grids:" user-id)}
        {:keys [data]} (faraday/get-item db/config :canary query)]
    (mapv #(update % :checked-dates set) data)))


(defmulti handle first)


(defmethod handle :profile [[_ {:keys [user-id current-user-id]}]]
  (if-let [user-id (or user-id current-user-id)]
    {:profile {user-id (query-profile user-id)}}
    {:profile {}}))


(defmethod handle :grids [[_ {:keys [user-id current-user-id]}]]
  (if-let [user-id (or user-id current-user-id)]
    {:grids {user-id (query-grids user-id)}}
    {:grids {}}))


(defmethod handle :authorisation-details [query]
  {:client-id (System/getenv "AUTHORISATION_CLIENT_ID")})


(defmethod handle :default [query]
  (throw (IllegalArgumentException. "Unsupported query method.")))
