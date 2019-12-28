(ns canary.handler
  (:require [canary.query :as query]
            [canary.command :as command]
            [canary.middleware :as middleware]
            [medley.core :as medley]
            [muuntaja.core :as muuntaja])
  (:import [com.amazonaws.services.lambda.runtime.RequestStreamHandler]
           [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class
   :name canary.Handler
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))


(defn read-input-stream
  [input-stream]
  (muuntaja/decode "application/json" input-stream))


(defn write-output-stream
  [output-stream response]
  (let [encoder (muuntaja/create (assoc muuntaja/default-options :return :bytes))]
    (.write output-stream (muuntaja/encode encoder "application/json" response))))


(defn handler [{:keys [handle body-params] :as request}]
  {:status 200
   :headers {}
   :body (apply medley/deep-merge (map handle body-params))})


(def app
  (-> handler
      (middleware/wrap-handle)
      (middleware/wrap-current-user-id)
      (middleware/wrap-content-type)
      (middleware/wrap-session)
      (middleware/wrap-cors)
      (middleware/wrap-adaptor)))


(defn -handleRequest
  [_ input-stream output-stream context]
  (try
    (->> input-stream
         (read-input-stream)
         (app)
         (write-output-stream output-stream))
    (catch Exception e
      (.printStackTrace e)
      (write-output-stream output-stream {:statusCode 500}))))
