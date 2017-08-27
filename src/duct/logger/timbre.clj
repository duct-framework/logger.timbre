(ns duct.logger.timbre
  (:require [duct.logger :as logger]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre]))

(defn brief-output-fn [{:keys [msg_]}]
  (force msg_))

(defn brief-appender [options]
  (-> (timbre/println-appender options)
      (assoc :output-fn brief-output-fn)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key ::println [_ options]
  (-> (timbre/println-appender options)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key ::spit [_ options]
  (-> (timbre/spit-appender options)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key ::brief [_ options]
  (brief-appender options))

(defn- duct-log-format? [vargs]
  (and (<= 1 (count vargs) 2)
       (let [[evt data] vargs]
         (and (keyword?  evt)
              (namespace evt)
              (or (nil? data)
                  (map? data))))))

(defn wrap-legacy-logs [{:keys [vargs] :as data}]
  (cond-> data
    (not (duct-log-format? vargs))
    (assoc :vargs [::legacy vargs])))

(defrecord TimbreLogger [config]
  logger/Logger
  (-log [_ level ns-str file line id event data]
    (cond
      (instance? Throwable data)
      (timbre/log! level :p (event)
                   {:config config, :?ns-str ns-str, :?file file, :?line line, :id_ id
                    :?err data})
      (nil? data)
      (timbre/log! level :p (event)
                   {:config config, :?ns-str ns-str, :?file file, :?line line, :id_ id})
      :else
      (timbre/log! level :p (event data)
                   {:config config, :?ns-str ns-str, :?file file, :?line line, :id_ id}))))

(defmethod ig/init-key :duct.logger/timbre [_ config]
  (let [timbre-logger (->TimbreLogger config)
        prev-root timbre/*config*]
    (if (:set-root-config? config)
      (let [config (update config :middleware (fnil conj []) wrap-legacy-logs)]
        (timbre/set-config! config)
        (assoc timbre-logger :prev-root-config prev-root))
      timbre-logger)))

(defmethod ig/halt-key! :duct.logger/timbre [_ timbre]
  (when-let [prev-config (:prev-root-config timbre)]
    (timbre/set-config! prev-config)))
