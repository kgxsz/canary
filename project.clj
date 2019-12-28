(defproject canary "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.amazonaws/aws-lambda-java-core "1.0.0"]
                 [com.taoensso/faraday "1.9.0"]
                 [metosin/muuntaja "0.6.6"]
                 [medley "1.2.0"]
                 [clj-http "3.10.0"]
                 [ring/ring-core "1.8.0"]
                 [ring-cors "0.1.13"]]
  :main canary.handler
  :target-path "target/"
  :uberjar-name "canary.jar"
  :source-paths ["src"]
  :profiles {:uberjar {:aot :all}})
