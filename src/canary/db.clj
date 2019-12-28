(ns canary.db
  (:require [taoensso.faraday :as faraday]))


(def config {:access-key (System/getenv "ACCESS_KEY")
             :secret-key (System/getenv "SECRET_KEY")
             :endpoint "http://dynamodb.eu-west-1.amazonaws.com"
             :batch-write-limit 25})
