(ns canary.handler
  (:require [canary.query :as query]
            [canary.command :as command]
            [medley.core :as medley]
            [muuntaja.core :as muuntaja])
  (:import [com.amazonaws.services.lambda.runtime.RequestStreamHandler]
           [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class
   :name canary.Handler
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))


(defn decode-input-stream
  [input-stream]
  (try
    (let [{:keys [body path]} (muuntaja/decode "application/json" input-stream)]
      {:request-body (muuntaja/decode "application/transit+json" body)
       :handle (case path "/query" query/handle "/command" command/handle)})
    (catch Exception e
      (throw (IllegalArgumentException. (.getMessage e))))))


(defn encode-output-stream
  [output-stream status-code response-body]
  (let [encoder (muuntaja/create (assoc muuntaja/default-options :return :bytes))
        response {:statusCode status-code
                  :headers {"Access-Control-Allow-Origin" "*"
                            "Content-Type" "application/transit+json"}
                  :body (slurp (muuntaja/encode "application/transit+json" response-body))}]
    (.write output-stream (muuntaja/encode encoder "application/json" response))))


(defn -handleRequest
  [_ input-stream output-stream context]
  (try
    (let [{:keys [request-body handle]} (decode-input-stream input-stream)
          response-body (apply medley/deep-merge (map handle request-body))]
      (encode-output-stream output-stream 200 response-body))
    (catch IllegalArgumentException e
      (.printStackTrace e)
      (encode-output-stream output-stream 400 {:error (.getMessage e)}))
    (catch Exception e
      (.printStackTrace e)
      (encode-output-stream output-stream 500 {:error (.getMessage e)}))))
