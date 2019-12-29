(ns canary.server
  (:require [canary.middleware :as middleware]
            [medley.core :as medley]
            [ring.adapter.jetty :as jetty]))


(def handler
  (-> (fn [{:keys [handle body-params]}]
        {:status 200
         :headers {}
         :body (apply medley/deep-merge (map handle body-params))})
      (middleware/wrap-handle)
      (middleware/wrap-current-user-id)
      (middleware/wrap-content-type)
      (middleware/wrap-session)
      (middleware/wrap-cors)))


(defn start-server []
  (let [options {:port 80 :join? false}]
    (jetty/run-jetty #'handler options)))


(defonce server (start-server))
