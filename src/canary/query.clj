(ns canary.query
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]))


(defn query-profile [user-id]
  (let [query {:partition (str "profile:" user-id)}
        {profile :data} (faraday/get-item db/config :canary query)]
    profile))


(defmulti handle first)


(defmethod handle :profile [[_ {:keys [user-id current-user-id]}]]
  {:profile (query-profile (or user-id current-user-id))})


(defmethod handle :grids [[_ {:keys [user-id current-user-id]}]]
  {:grids []})


(defmethod handle :authorisation-details [query]
  {:client-id (System/getenv "AUTHORISATION_CLIENT_ID")})


(defmethod handle :default [query]
  (throw (IllegalArgumentException. "Unsupported query method.")))
