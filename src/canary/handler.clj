(ns canary.handler
  (:require [taoensso.faraday :as faraday]
            [medley.core :as medley]
            [clj-uuid :as uuid]
            [clj-time.core :as time]
            [clj-time.coerce :as time.coerce]
            [muuntaja.core :as muuntaja])
  (:import [com.amazonaws.services.lambda.runtime.RequestStreamHandler]
           [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class
   :name canary.Handler
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(def ^:const +namespace+ #uuid "cc96ca9d-2ce4-49a4-a5c6-801291865907")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Config ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config {:dynamodb {:access-key (System/getenv "ACCESS_KEY")
                        :secret-key (System/getenv "SECRET_KEY")
                        :endpoint "http://dynamodb.eu-west-1.amazonaws.com"
                        :batch-write-limit 25}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti handle-query first)

(defmethod handle-query :profile [[_ {:keys [user]}]]
  {:profile (faraday/get-item
             (:dynamodb config)
             :canary
             {:partition (str user ":profile")
              :sort 123456})})

(defmethod handle-query :default [query]
  (throw (IllegalArgumentException. "Unsupported query method.")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Command ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti handle-command first)

(defmethod handle-command :add-profile [[_ {:keys [user profile]}]]
 (let [item {:partition (str user ":profile")
              :sort 123456
              :profile profile}]
    (faraday/put-item (:dynamodb config) :canary item)
    {}))

(defmethod handle-command :default [command]
  (throw (IllegalArgumentException. "Unsupported command method.")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn encode-output-stream [output-stream status-code response-body]
  (let [encoder (muuntaja/create (assoc muuntaja/default-options :return :bytes))
        response {:statusCode status-code
                  :headers {"Access-Control-Allow-Origin" "*"
                            "Content-Type" "application/json"}
                  :body (slurp (muuntaja/encode "application/json" response-body))}]
    (.write output-stream (muuntaja/encode encoder "application/json" response))))


(defn decode-input-stream [input-stream]
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
                   "/query" handle-query
                   "/command" handle-command
                   (throw (IllegalArgumentException. "Unsupported path parameter.")))
          response-body (or (apply medley/deep-merge (map handle body)) {})]
      (encode-output-stream output-stream 200 response-body))
    (catch IllegalArgumentException e
      (.printStackTrace e)
      (encode-output-stream output-stream 400 {:error (.getMessage e)}))
    (catch Exception e
      (.printStackTrace e)
      (encode-output-stream output-stream 500 {:error (.getMessage e)}))))
