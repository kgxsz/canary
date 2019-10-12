(ns canary.command
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]))


(defmulti handle first)


(defmethod handle :add-profile [[_ {:keys [user profile]}]]
 (let [item {:partition (str user ":profile")
              :sort 123456
              :profile profile}]
   (faraday/put-item db/config :canary item)
   {}))


(defmethod handle :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))
