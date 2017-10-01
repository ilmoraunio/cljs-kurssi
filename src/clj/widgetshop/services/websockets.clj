(ns widgetshop.services.websockets
  (:require [widgetshop.components.http :refer [publish! transit-response]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes GET POST]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ;; Dispatch based on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event ?reply-fn]}]
  (-event-msg-handler ev-msg) ;; handle events on a single thread
  ;; (future (-event-msg-handler ev-msg)) ;; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when ?reply-fn
    (?reply-fn {:umatched-event-as-echoed-from-from-server event})))

(defmethod -event-msg-handler :example/send-message
  [{:as ev-msg}]
  (prn "received message" ev-msg))

(defmethod -event-msg-handler :example/send-and-receive-response
  [{:as ev-msg :keys [?reply-fn]}]
  (?reply-fn {:payload #{1 2 3}}))

(defmethod -event-msg-handler :example/broadcast-message
  [{:as ev-msg :keys [send-fn]}]
  (send-fn :sente/all-users-without-uid [:example/broadcast-message]))

(defrecord WebsocketsService []
  component/Lifecycle
  (start [{:keys [http] :as this}]
    (let [{:keys [ch-recv        send-fn
                  connected-uids ajax-post-fn
                  ajax-get-or-ws-handshake-fn] :as channel-socket}
      (sente/make-channel-socket! (get-sch-adapter) {})]
      (assoc this ::ws-routes
             (sente/start-server-chsk-router!
               ch-recv event-msg-handler))
      (assoc this ::routes
             (publish! http
                       (routes
                        (GET "/chsk" req 
                          (ajax-get-or-ws-handshake-fn req))
                        (POST "/chsk" req 
                          (ajax-post-fn                req)))))))
  (stop [{stop ::routes :as this}]
    (stop)
    (dissoc this ::routes ::ws-routes)))