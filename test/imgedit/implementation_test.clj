(ns imgedit.implementation-test
  (:require [imgedit.implementation :as sut]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.spec.test.alpha :as stest]))

(defn- check [function num-tests]
  (if num-tests
    (stest/check function {:clojure.spec.test.check/opts {:num-tests num-tests}})
    (stest/check function)))

(defn checking
  ([function]
   (checking function nil))
  ([function num-tests]
   (testing "Schema"
     (let [result (-> (check function num-tests)
                    first
                    :clojure.spec.test.check/ret
                    :result)]
       (cond
         (true? result) (is result)
         (nil? (ex-data result)) (is (= {} result))
         :else (is (= {} (ex-data result))))))))

(defn fails-spec [f & args]
  (try
    (let [res (apply f args)]
      (is (instance? Exception res)))
    (catch Exception e
      (is (contains? (ex-data e) :clojure.spec/problems)))))

;; exercising the implementation functions with the fdef generators in imgedit.specs


(deftest new-image
  (checking `sut/new-image))

(deftest clear
  (checking `sut/clear))

(deftest get-colour
  (checking `sut/get-colour))

(deftest pixel
  (checking `sut/pixel) )

(deftest vertical-line
  (checking `sut/vertical-line) )

(deftest horizontal-line
  (checking `sut/horizontal-line) )

(deftest matching-neighbours
  (checking `sut/matching-neighbours) )

(deftest fill
  (checking `sut/fill 20) )

(deftest show
  (with-out-str (checking `sut/show 5)))
