(ns canary.server
  (:require [canary.middleware :as middleware]
            [medley.core :as medley]
            [ring.adapter.jetty :as jetty]))


(defn handler [{:keys [handle body-params] :as request}]
  (let [response-body (apply medley/deep-merge (map handle body-params))]
    {:status 404
     :headers {}
     :body response-body}))


(def app
  (-> handler
      (middleware/wrap-handle)
      (middleware/wrap-current-user-id)
      (middleware/wrap-content-type)
      (middleware/wrap-session)
      (middleware/wrap-cors)))


(defn start-server []
  (let [options {:port 80 :join? false}]
    (jetty/run-jetty #'app options)))


(defonce server (start-server))
