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
    (is (= (with-out-str (logger/log logger :info ::testing {:foo "bar"}))
           ":duct.logger.timbre-test/testing {:foo \"bar\"}\n"))))
