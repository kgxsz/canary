(ns canary.query
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]))


(defn query-profile [user-id]
  (let [query {:partition (str "profile:" user-id)}]
    (:data (faraday/get-item db/config :canary query))))


(defn query-grids [user-id]
  (let [query {:partition (str "grids:" user-id)}]
    (:data (faraday/get-item db/config :canary query))))


(defmulti handle first)


(defmethod handle :profile [[_ {:keys [user-id current-user-id]}]]
  (let [user-id (or user-id current-user-id)]
    {:profile {user-id (query-profile user-id)}}))


(defmethod handle :grids [[_ {:keys [user-id current-user-id]}]]
  (let [user-id (or user-id current-user-id)]
    {:grids {user-id (query-grids user-id)}}))


(defmethod handle :authorisation-details [query]
  {:client-id (System/getenv "AUTHORISATION_CLIENT_ID")})


(defmethod handle :default [query]
  (throw (IllegalArgumentException. "Unsupported query method.")))
