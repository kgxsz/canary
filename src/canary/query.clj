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


(defmethod handle :default [query]
  (throw (IllegalArgumentException. "Unsupported query method.")))
