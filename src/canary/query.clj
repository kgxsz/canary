(ns canary.query
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]))


(defmulti handle first)


(defmethod handle :profile [[_ {:keys [user]}]]
  {:profile (faraday/get-item
             db/config
             :canary
             {:partition (str user ":profile")
              :sort 123456})})


(defmethod handle :authorisation-details [[_ _]]
  {:client-id "8d06f025e5fbd7809f2b"})


(defmethod handle :default [query]
  (throw (IllegalArgumentException. "Unsupported query method.")))
