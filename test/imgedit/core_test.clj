(ns imgedit.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [imgedit.implementation :refer [new-image]]
            [imgedit.core :refer [pump*]]))

(deftest integration-tests
  (testing "Intro.md examples"
    (is (= (with-out-str
             (-> (new-image 10 10)
               (pump* "I 5 6")
               (pump* "L 2 3 A")
               (pump* "S")))
          "OOOOO\nOOOOO\nOAOOO\nOOOOO\nOOOOO\nOOOOO\n")
      "A line is drawn at the right location.")
    (is (= (with-out-str
             (-> (new-image 10 10)
               (pump* "I 5 6")
               (pump* "L 2 3 A")
               (pump* "F 3 3 J")
               (pump* "V 2 3 4 W")
               (pump* "H 3 4 2 Z")
               (pump* "S")))
          "JJJJJ\nJJZZJ\nJWJJJ\nJWJJJ\nJJJJJ\nJJJJJ\n")
      "The full example in the spec doc renders correctly.")))

