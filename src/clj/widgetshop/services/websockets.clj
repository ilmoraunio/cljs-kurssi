(ns widgetshop.services.websockets
  (:require [widgetshop.components.http :refer [publish! transit-response]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes GET POST]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(let [{:keys [ch-recv 
              send-fn 
              connected-uids
              ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defrecord WebsocketsService []
  component/Lifecycle
  (start [{:keys [http] :as this}]
    (assoc this ::routes
           (publish! http
                     (routes
                      (GET "/chsk" req 
                        (ring-ajax-get-or-ws-handshake req))
                      (POST "/chsk" req 
                        (ring-ajax-post                req))))))
  (stop [{stop ::routes :as this}]
    (stop)
    (dissoc this ::routes)))