(ns duct.logger.timbre-test
  (:require [clojure.test :refer :all]
            [duct.logger :as logger]
            [duct.logger.timbre :as timbre]
            [taoensso.timbre :as tao]
            [integrant.core :as ig]))

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
         #"(?x)\d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+
           \ INFO\ \[duct\.logger\.timbre-test:\d\d\]\ -
           \ :duct\.logger\.timbre-test/testing\n"
         (with-out-str (logger/log logger :info ::testing))))
    (is (re-matches
         #"(?x)\d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+
          \ WARN\ \[duct\.logger\.timbre-test:\d\d\]\ -
          \ :duct\.logger\.timbre-test/testing\ \{:foo\ \"bar\"\}\n"
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
           \d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+
           \ INFO\ \[duct\.logger\.timbre-test:\d\d\]\ -
           \ :duct\.logger\.timbre-test/testing\n

           \d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+
           \ WARN\ \[duct\.logger\.timbre-test:\d\d\]\ -
           \ :duct\.logger\.timbre-test/testing\ \{:foo\ \"bar\"\}\n"
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
           #"(?x)\d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+
           \ REPORT\ \[duct\.logger\.timbre-test:\d\d\]\ -
           \ :duct\.logger\.timbre-test/testing\n"
           (with-out-str (logger/log logger :report ::testing))))))

  (testing "spit appender"
    (let [tempfile (doto (java.io.File/createTempFile "timbre" "log") (.deleteOnExit))
          config   {::logger/timbre {:level :info, :appenders {:spit (ig/ref ::timbre/spit)}}
                    ::timbre/spit   {:fname (str tempfile), :min-level :report}}
          logger   (::logger/timbre (ig/init config))]
      (logger/log logger :info ::testing)
      (logger/log logger :report ::testing)
      (is (re-matches
           #"(?x)
             \d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+
             \ REPORT\ \[duct\.logger\.timbre-test:\d\d\]\ -
             \ :duct\.logger\.timbre-test/testing\n"
           (slurp tempfile))))))

(deftest restore-root-timbre-config-test
  (let [init-binding tao/*config*
        config {::logger/timbre {:level :info, :appenders {}}}] 
    (try
      (let [system (ig/init config)]
        (is (= (::logger/timbre config) tao/*config*))
        (ig/halt! system)
        (is (= init-binding tao/*config*)))
      (finally
        (tao/set-config! init-binding)))))
