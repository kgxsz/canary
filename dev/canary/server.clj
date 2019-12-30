(ns canary.server
  (:require [canary.middleware :as middleware]
            [medley.core :as medley]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]))


(def handler
  (-> (fn [request]
        (->> (:body-params request)
             (map (:handle request))
             (apply medley/deep-merge)
             (response/response)))
      (middleware/wrap-handle)
      (middleware/wrap-current-user-id)
      (middleware/wrap-content-type)
      (middleware/wrap-session)
      (middleware/wrap-cors)
      (middleware/wrap-exception)))


(defn start-server []
  (let [options {:port 80 :join? false}]
    (jetty/run-jetty #'handler options)))


(defonce server (start-server))
