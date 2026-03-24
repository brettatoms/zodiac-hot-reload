(ns zodiac.ext.hot-reload
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.hot-reload.core :as hot]))

(create-ns 'zodiac.core)
(alias 'z 'zodiac.core)

(defmethod ig/init-key ::hot-reload [_ opts]
  (log/debug "Starting hot reload...")
  (let [reloader (hot/hot-reloader opts)
        watcher-handle (hot/start! reloader)]
    (log/info (str "Hot reload active, watching: " (pr-str (:watch-paths opts))))
    (log/info "For REPL-eval reloading, add to .nrepl.edn: {:middleware ^:concat [ring.hot-reload.nrepl/wrap-hot-reload-nrepl]}")
    (assoc reloader ::watcher-handle watcher-handle)))

(defmethod ig/halt-key! ::hot-reload [_ reloader]
  (log/debug "Stopping hot reload...")
  (hot/stop! reloader (::watcher-handle reloader)))

(defmethod ig/init-key ::middleware [_ {:keys [hot-reload]}]
  (let [{:keys [ws-handler injection-middleware uri-prefix]} hot-reload]
    (fn [handler]
      (let [inject-handler (injection-middleware handler)]
        (fn
          ([request]
           (if (= (:uri request) uri-prefix)
             (ws-handler request)
             (inject-handler request)))
          ([request respond raise]
           (if (= (:uri request) uri-prefix)
             (ws-handler request respond raise)
             (inject-handler request respond raise))))))))

(defn init
  "Creates a Zodiac extension for hot reload. Returns a config transformer
   function suitable for use in `:extensions`.

   Options are passed directly to `ring.hot-reload.core/hot-reloader`:
     :watch-paths      - directories to watch (default [\"src\"])
     :watch-extensions - file extensions that trigger reload
                         (default #{\".clj\" \".cljc\" \".edn\" \".html\" \".css\"})
     :uri-prefix       - WebSocket endpoint path (default \"/__hot-reload\")
     :inject?          - predicate (fn [request response]) controlling script
                         injection (default: always inject into HTML responses)
     :debounce-ms      - debounce window in ms (default 100)
     :bust-css-cache?  - append cache-busting param to stylesheet URLs on
                         reload (default false)

   Usage:
     (z/start {:extensions [(z.hot-reload/init)]})
     (z/start {:extensions [(z.hot-reload/init {:watch-paths [\"src\" \"resources/templates\"]})]})"
  ([] (init {}))
  ([opts]
   (fn [config]
     (-> config
         (assoc ::hot-reload opts)
         (assoc ::middleware {:hot-reload (ig/ref ::hot-reload)})
         (update-in [::z/app :user-middleware]
                    #(vec (cons (ig/ref ::middleware) %)))))))
