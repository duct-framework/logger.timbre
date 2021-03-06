(ns duct.logger.timbre-test
  (:require [clojure.test :refer :all]
            [duct.core :as duct]
            [duct.logger :as logger]
            [duct.logger.timbre :as timbre]
            [integrant.core :as ig]
            [taoensso.timbre :as tao]))

(duct/load-hierarchy)

(deftest key-test
  (is (isa? :duct.logger/timbre :duct/logger)))

(deftest brief-appender-test
  (let [config {::logger/timbre {:level :info, :appenders {:brief (ig/ref ::timbre/brief)}}
                ::timbre/brief  {}}
        logger (::logger/timbre (ig/init config))]
    (is (= (with-out-str (logger/log logger :info ::testing))
           ":duct.logger.timbre-test/testing\n"))
    (is (= (with-out-str (logger/log logger :warn ::testing {:foo "bar"}))
           ":duct.logger.timbre-test/testing {:foo \"bar\"}\n"))))

(deftest println-appender-test
  (let [config {::logger/timbre  {:level :info, :appenders {:prn (ig/ref ::timbre/println)}}
                ::timbre/println {}}
        logger (::logger/timbre (ig/init config))]
    (is (re-matches
         #"(?x)\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z\s[^\s]+
           \sINFO\s\[duct\.logger\.timbre-test:\d\d\]\s-
           \s:duct\.logger\.timbre-test/testing\n"
         (with-out-str (logger/log logger :info ::testing))))
    (is (re-matches
         #"(?x)\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z\s[^\s]+
          \sWARN\s\[duct\.logger\.timbre-test:\d\d\]\s-
          \s:duct\.logger\.timbre-test/testing\ \{:foo\ \"bar\"\}\n"
         (with-out-str (logger/log logger :warn ::testing {:foo "bar"}))))))

(deftest spit-appender-test
  (let [tempfile (doto (java.io.File/createTempFile "timbre" "log") (.deleteOnExit))
        config   {::logger/timbre {:level :info, :appenders {:spit (ig/ref ::timbre/spit)}}
                  ::timbre/spit   {:fname (str tempfile)}}
        logger   (::logger/timbre (ig/init config))]
    (logger/log logger :info ::testing)
    (logger/log logger :warn ::testing {:foo "bar"})
    (is (re-matches
         #"(?x)
           \d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z\s[^\s]+
           \sINFO\s\[duct\.logger\.timbre-test:\d\d\]\s-
           \s:duct\.logger\.timbre-test/testing\n

           \d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z\s[^\s]+
           \sWARN\s\[duct\.logger\.timbre-test:\d\d\]\s-
           \s:duct\.logger\.timbre-test/testing\ \{:foo\ \"bar\"\}\n"
         (slurp tempfile)))))

(deftest min-level-test
  (testing "brief appender"
    (let [config {::logger/timbre {:level :info, :appenders {:brief (ig/ref ::timbre/brief)}}
                  ::timbre/brief  {:min-level :report}}
          logger (::logger/timbre (ig/init config))]
      (is (= (with-out-str (logger/log logger :info ::testing)) ""))
      (is (= (with-out-str (logger/log logger :report ::testing))
             ":duct.logger.timbre-test/testing\n"))))

  (testing "println appender"
    (let [config {::logger/timbre  {:level :info, :appenders {:prn (ig/ref ::timbre/println)}}
                  ::timbre/println {:min-level :report}}
          logger (::logger/timbre (ig/init config))]
      (is (= (with-out-str (logger/log logger :info ::testing)) ""))
      (is (re-matches
           #"(?x)\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z\s[^\s]+
           \sREPORT\s\[duct\.logger\.timbre-test:\d\d\]\s-
           \s:duct\.logger\.timbre-test/testing\n"
           (with-out-str (logger/log logger :report ::testing))))))

  (testing "spit appender"
    (let [tempfile (doto (java.io.File/createTempFile "timbre" "log") (.deleteOnExit))
          config   {::logger/timbre {:level :info, :appenders {:spit (ig/ref ::timbre/spit)}}
                    ::timbre/spit   {:fname (str tempfile), :min-level :report}}
          logger   (::logger/timbre (ig/init config))]
      (logger/log logger :info ::testing)
      (logger/log logger :report ::testing)
      (is (re-matches
           #"(?x)\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z\s[^\s]+
             \sREPORT\s\[duct\.logger\.timbre-test:\d\d\]\s-
             \s:duct\.logger\.timbre-test/testing\n"
           (slurp tempfile))))))

(deftest restore-root-timbre-config-test
  (let [prev-log-config tao/*config*
        config {::timbre/brief  {:min-level :report}
                ::logger/timbre {:level :info
                                 :appenders {:brief (ig/ref ::timbre/brief)}
                                 :set-root-config? true}}]
    (try
      (let [system     (ig/init config)
            log-config (:config (::logger/timbre system))]
        (is (= (assoc log-config :middleware [timbre/wrap-legacy-logs]) tao/*config*))
        (is (= (with-out-str
                 (tao/report ::foo)
                 (tao/report ::foo {:x 1})
                 (tao/report ::foo [:x 1])
                 (tao/report "test")
                 (tao/report "test" 1 2 3))
               (str ::foo "\n"
                    ::foo " " {:x 1} "\n"
                    ::timbre/legacy " " [::foo [:x 1]] "\n"
                    ::timbre/legacy " " ["test"] "\n"
                    ::timbre/legacy " " ["test" 1 2 3] "\n")))
        (ig/halt! system)
        (is (= prev-log-config tao/*config*)))
      (finally
        (tao/set-config! prev-log-config)))))

(defn test-id-appender [options]
  (-> (tao/println-appender options)
      (assoc :output-fn (fn [{:keys [id_]}] (str (force id_))))))

(deftest logging-id-test
  (let [config {::logger/timbre {:level :info, :appenders {:brief (test-id-appender {})}}}
        logger (::logger/timbre (ig/init config))]
    (is (re-matches
         #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\n"
         (with-out-str (logger/log logger :info ::testing))))))
