(ns widgetshop.server
  "Server communication"
  (:require [ajax.core :as ajax]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn get! [url {:keys [params on-success on-failure]}]
  (ajax/GET url {:params params
                 :handler on-success
                 :error-handler on-failure
                 :response-format :transit}))

;; Sente init, handlers & routes

(defmulti chsk-routes (fn [{:as ev-msg :keys [event]}] 
  (let [[id [broadcast-event]] event] 
    broadcast-event)))

(defmethod chsk-routes :example/broadcast-message
  [{:as ev-msg :keys [?data]}]
  (println "Broadcast message received"))

(defmulti -event-msg-handler
  "Multimethod wrapper to handle Sente `event-msg`s"
  :id)
(defmethod -event-msg-handler :chsk/state [_])
(defmethod -event-msg-handler :chsk/handshake [_])
(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg}]
  (chsk-routes ev-msg))

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(let [{:keys [chsk
              ch-recv
              send-fn
              state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
       {:type :auto ; e/o #{:auto :ajax :ws}
       })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(sente/start-client-chsk-router! ch-chsk event-msg-handler)