(ns widgetshop.server
  "Server communication"
  (:require [ajax.core :as ajax]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn get! [url {:keys [params on-success on-failure]}]
  (ajax/GET url {:params params
                 :handler on-success
                 :error-handler on-failure
                 :response-format :transit}))

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