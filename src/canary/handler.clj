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


(defn encode-output-stream
  [output-stream status-code response-body]
  (let [encoder (muuntaja/create (assoc muuntaja/default-options :return :bytes))
        response {:statusCode status-code
                  :headers {"Access-Control-Allow-Origin" "*"
                            "Content-Type" "application/json"}
                  :body (slurp (muuntaja/encode "application/json" response-body))}]
    (.write output-stream (muuntaja/encode encoder "application/json" response))))


(defn decode-input-stream
  [input-stream]
  (try
    (-> (muuntaja/decode "application/json" input-stream)
        (update :body (partial muuntaja/decode "application/json")))
    (catch Exception e
      (throw (IllegalArgumentException. "The input stream is malformed.")))))


(defn -handleRequest
  [_ input-stream output-stream context]
  (try
    (let [{:keys [body path]} (decode-input-stream input-stream)
          handle (case path
                   "/query" query/handle
                   "/command" command/handle
                   (throw (IllegalArgumentException. "Unsupported path parameter.")))
          response-body (or (apply medley/deep-merge (map handle body)) {})]
      (encode-output-stream output-stream 200 response-body))
    (catch IllegalArgumentException e
      (.printStackTrace e)
      (encode-output-stream output-stream 400 {:error (.getMessage e)}))
    (catch Exception e
      (.printStackTrace e)
      (encode-output-stream output-stream 500 {:error (.getMessage e)}))))
