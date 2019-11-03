(ns canary.command
  (:require [canary.db :as db]
            [taoensso.faraday :as faraday]
            [clj-http.client :as client]))


(defmulti handle first)


(defmethod handle :add-profile [[_ {:keys [user profile]}]]
 (let [item {:partition (str user ":profile")
              :sort 123456
              :profile profile}]
   (faraday/put-item db/config :canary item)
   {}))


(defmethod handle :verify-authorisation [[_ {:keys [code]}]]
  #_(client/post "https://github.com/login/oauth/access_token"
               {:body (slurp (muuntaja/encode "application/json"
                                              {:client-id "1234"
                                               :client-secret "1234"
                                               :code code}))
                :content-type :json
                :accept :json})
  ;; make connection
  ;; create session
  {:code code})


(defmethod handle :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))
