(ns duct.logger.timbre-test
  (:require [clojure.test :refer :all]
            [duct.logger :as logger]
            [duct.logger.timbre :as timbre]
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
         #"(?x)\d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+\ 
           INFO\ \[duct\.logger\.timbre-test:27\]\ -\ 
           :duct\.logger\.timbre-test/testing\n"
         (with-out-str (logger/log logger :info ::testing))))
    (is (re-matches
         #"(?x)\d\d-\d\d-\d\d\ \d\d:\d\d:\d\d\ [^\s]+\ 
           WARN\ \[duct\.logger\.timbre-test:32\]\ -\ 
           :duct\.logger\.timbre-test/testing\ \{:foo\ \"bar\"\}\n"
         (with-out-str (logger/log logger :warn ::testing {:foo "bar"}))))))
