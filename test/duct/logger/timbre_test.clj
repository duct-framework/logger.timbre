(ns duct.logger.timbre-test
  (:require [clojure.test :refer :all]
            [duct.logger :as logger]
            duct.logger.timbre))

(deftest key-test
  (is (isa? :duct.logger/timbre :duct/logger)))
