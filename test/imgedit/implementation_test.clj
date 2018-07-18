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
  (checking `sut/fill 10)
  (let [test-image (-> (sut/new-image 11 11)
                     (sut/vertical-line [:X 6] [:YS 1 11] [:COLOUR \L])
                     (sut/fill [:X 3] [:Y 3] [:COLOUR \A])
                     (sut/fill [:X 8] [:Y 8] [:COLOUR \B])
                     (sut/fill [:X 6] [:Y 6] [:COLOUR \C]))
        count-pixels (fn [image c]
                       (count (filter #(= % c) (vals (:image/pixels image)))))]
    (testing
      (is (= (count-pixels test-image \A) 55)
        "The left side of the test image has the expected number of pixels coloured A'.")
      (is (= (count-pixels test-image \B) 55)
        "The right side of the test image has the expected number of pixels coloured 'B'.")
      (is (= (count-pixels test-image \C) 11)
        "The dividing line of the test image has the expected number of pixels coloured 'C'"))))

(deftest show
  (with-out-str (checking `sut/show 5)))
