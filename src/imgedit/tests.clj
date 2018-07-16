(ns imgedit.tests
  (:require [imgedit.tests :as sut]
            [imgedit.implementation :as impl]
            [imgedit.specs :as is]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.spec.test.alpha :as stest]))

;; utils to integrate generative testing with clojure.test
;; see https://gist.github.com/jmglov/30571ede32d34208d77bebe51bb64f29

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



(deftest new-image
  (checking `impl/new-image))

(deftest clear
  (checking `impl/clear))

(deftest pixel
  (checking `impl/get-colour))

(deftest pixel
  (checking `impl/pixel) )

(deftest pixel
  (checking `impl/vertical-line) )

(deftest pixel
  (checking `impl/horizontal-line) )

(deftest pixel
  (checking `impl/matching-neighbours) )

(deftest pixel
  (checking `impl/fill 20) )

(deftest show
  (with-out-str (checking `impl/show)))

