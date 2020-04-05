(ns canary.handler
  (:require [canary.query :as query]
            [canary.command :as command]
            [canary.middleware :as middleware]
            [ring.util.response :as response]
            [medley.core :as medley]
            [muuntaja.core :as muuntaja])
  (:import [com.amazonaws.services.lambda.runtime RequestStreamHandler]
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
      (middleware/wrap-exception)
      (middleware/wrap-adaptor)))


(defn -handleRequest
  [_ input-stream output-stream context]
  (try
    (->> input-stream
         (read-input-stream)
         (handler)
         (write-output-stream output-stream))
    (catch Exception e
      (.printStackTrace e)
      (write-output-stream output-stream {:statusCode 500}))))
