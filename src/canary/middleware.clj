(ns canary.middleware
  (:require [canary.query :as query]
            [canary.command :as command]
            [ring.middleware.cors :as cors.middleware]
            [ring.middleware.session :as session.middleware]
            [ring.middleware.session.cookie :as cookie]
            [muuntaja.middleware :as muuntaja.middleware]
            [medley.core :as medley]
            [clojure.java.io :as io]))


(defn wrap-handle
  "Determines whether to use the query/handle or command/handle function,
   and adds it to the request to be used by the handler."
  [handler]
  (fn [{:keys [uri] :as request}]
    (let [handle (case uri
                   "/query" query/handle
                   "/command" command/handle)
          request (assoc request :handle handle)]
      (handler request))))


(defn wrap-current-user-id
  "For inbound requests, takes the current user id found in the session
   and adds it to every query/command. For outbound responses, checks
   whether the current user id has been updated or removed, and acts
   accordingly by updating or removing the session."
  [handler]
  (fn [{:keys [body-params session] :as request}]
    (let [{:keys [current-user-id]} session
          body-params (medley/map-vals
                       #(assoc % :current-user-id current-user-id)
                       body-params)
          request (assoc request :body-params body-params)
          {:keys [body] :as response} (handler request)]
      (if (contains? body :current-user-id)
        (if-let [current-user-id (get-in response [:body :current-user-id])]
          (assoc-in response [:session :current-user-id] current-user-id)
          (assoc response :session nil))
        (assoc-in response [:body :current-user-id] current-user-id)))))


(defn wrap-content-type
  "Formats the inbound request and outbound response based on the content type header."
  [handler]
  (muuntaja.middleware/wrap-format handler))


(defn wrap-session
  "Handles the session, using an encrypted cookie to store the session's
   state in the client."
  [handler]
  (session.middleware/wrap-session
   handler
   {:cookie-name "session"
    :cookie-attrs {:max-age 120
                   :domain (System/getenv "COOKIE_ATTRIBUTE_DOMAIN")
                   :path "/"
                   :http-only true
                   :same-site :none
                   :secure (Boolean/parseBoolean (System/getenv "COOKIE_ATTRIBUTE_SECURE"))}
    :store (cookie/cookie-store {:key (System/getenv "COOKIE_STORE_KEY")})}))


(defn wrap-cors
  "Handles all the cross origin resource sharing concerns."
  [handler]
  (cors.middleware/wrap-cors
   handler
   :access-control-allow-origin [(re-pattern (System/getenv "CORS_ORIGIN"))]
   :access-control-allow-methods [:options :post]
   :access-control-allow-credentials "true"))


(defn wrap-adaptor
  "Handlers the adaption between Ring and AWS Lambda."
  [handler]
  (fn [{:keys [headers path requestContext body] :as request}]
    (let [{:keys [X-Forwarded-Port X-Forwarded-For X-Forwarded-Proto Host]} headers
          request {:server-port (Integer/parseInt X-Forwarded-Port)
                   :server-name Host
                   :remote-addr (first (clojure.string/split X-Forwarded-For #", "))
                   :uri path
                   :scheme (keyword X-Forwarded-Proto)
                   :protocol (:protocol requestContext)
                   :headers (medley/map-keys
                             (comp clojure.string/lower-case name)
                             headers)
                   :request-method (-> (:httpMethod request)
                                       (clojure.string/lower-case)
                                       (keyword))
                   :body (some-> body (.getBytes) io/input-stream)
                   :query-string (:queryStringParameters request)}
          {:keys [status headers body] :as response} (handler request)]
      {:statusCode status
       :headers (update headers "Set-Cookie" first)
       :body (slurp body)})))
