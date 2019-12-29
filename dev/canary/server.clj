(ns canary.server
  (:require [canary.handler :as handler]
            [canary.middleware :as middleware]
            [ring.adapter.jetty :as jetty]))


(def app
  (-> handler/handler
      (middleware/wrap-handle)
      (middleware/wrap-current-user-id)
      (middleware/wrap-content-type)
      (middleware/wrap-session)
      (middleware/wrap-cors)))


(defn start-server []
  (let [options {:port 80 :join? false}]
    (jetty/run-jetty #'app options)))


(defonce server (start-server))
