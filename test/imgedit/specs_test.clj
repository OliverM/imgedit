(ns imgedit.specs-test
  (:require [imgedit.specs :as sut]
            [imgedit.implementation :refer [new-image]]
            [clojure.spec.alpha :as s]
            [clojure.test :as t :refer [deftest testing is are]]))

(deftest valid-commands
  (are [command] (s/valid? ::sut/command command)
    [:COMMAND [:SHOW]]
    [:COMMAND [:CLEAR]]
    [:COMMAND [:QUIT]]
    [:COMMAND [:NEW [:WIDTH 10] [:HEIGHT 10]]]
    [:COMMAND [:PIXEL [:X 10] [:Y 10] [:COLOUR \i]]]
    [:COMMAND [:VERTICAL [:X 5] [:YS 1 10] [:COLOUR \x]]]
    [:COMMAND [:HORIZONTAL [:XS 1 10] [:Y 5] [:COLOUR \y]]]
    [:COMMAND [:FILL [:X 5] [:Y 5] [:COLOUR \f]]]))

(deftest invalid-command-dimensions
  (are [command] (not (s/valid? ::sut/command command))
    [:COMMAND [:NEW [:WIDTH 0] [:HEIGHT 10]]]
    [:COMMAND [:NEW [:WIDTH 10] [:HEIGHT 0]]]
    [:COMMAND [:NEW [:WIDTH 10] [:HEIGHT 251]]]
    [:COMMAND [:NEW [:WIDTH 251] [:HEIGHT 10]]]
    [:COMMAND [:PIXEL [:X 0] [:Y 10] [:COLOUR \i]]]
    [:COMMAND [:PIXEL [:X 251] [:Y 10] [:COLOUR \i]]]
    [:COMMAND [:PIXEL [:X 10] [:Y 0] [:COLOUR \i]]]
    [:COMMAND [:PIXEL [:X 10] [:Y 251] [:COLOUR \i]]]
    [:COMMAND [:VERTICAL [:X 0] [:YS 1 10] [:COLOUR \x]]]
    [:COMMAND [:VERTICAL [:X 251] [:YS 1 10] [:COLOUR \x]]]
    [:COMMAND [:VERTICAL [:X 5] [:YS 1 0] [:COLOUR \x]]]
    [:COMMAND [:VERTICAL [:X 5] [:YS 1 251] [:COLOUR \x]]]
    [:COMMAND [:VERTICAL [:X 5] [:YS 0 10] [:COLOUR \x]]]
    [:COMMAND [:VERTICAL [:X 5] [:YS 251 10] [:COLOUR \x]]]
    [:COMMAND [:HORIZONTAL [:XS 0 10] [:Y 5] [:COLOUR \y]]]
    [:COMMAND [:HORIZONTAL [:XS 251 10] [:Y 5] [:COLOUR \y]]]
    [:COMMAND [:HORIZONTAL [:XS 1 0] [:Y 5] [:COLOUR \y]]]
    [:COMMAND [:HORIZONTAL [:XS 1 251] [:Y 5] [:COLOUR \y]]]
    [:COMMAND [:HORIZONTAL [:XS 1 10] [:Y 0] [:COLOUR \y]]]
    [:COMMAND [:HORIZONTAL [:XS 1 10] [:Y 251] [:COLOUR \y]]]
    [:COMMAND [:FILL [:X 0] [:Y 5] [:COLOUR \f]]]
    [:COMMAND [:FILL [:X 251] [:Y 5] [:COLOUR \f]]]
    [:COMMAND [:FILL [:X 5] [:Y 0] [:COLOUR \f]]]
    [:COMMAND [:FILL [:X 5] [:Y 251] [:COLOUR \f]]]))

(deftest valid-image-command-dimensions
  (let [image (new-image 10 10)]
    (are [image command] (s/valid? ::sut/image-command [image command])
      image [:COMMAND [:PIXEL [:X 1] [:Y 10] [:COLOUR \i]]]
      image [:COMMAND [:VERTICAL [:X 5] [:YS 1 10] [:COLOUR \x]]]
      image [:COMMAND [:HORIZONTAL [:XS 1 10] [:Y 5] [:COLOUR \y]]]
      image [:COMMAND [:FILL [:X 5] [:Y 5] [:COLOUR \i]]])))

(deftest invalid-image-command-dimensions
  (let [image (new-image 10 10)]
    (are [image command] (not (s/valid? ::sut/image-command [image command]))
      image [:COMMAND [:PIXEL [:X 0] [:Y 10] [:COLOUR \i]]]
      image [:COMMAND [:VERTICAL [:X 5] [:YS 1 50] [:COLOUR \x]]]
      image [:COMMAND [:HORIZONTAL [:XS 1000 10] [:Y 5] [:COLOUR \y]]]
      image [:COMMAND [:FILL [:X 5] [:Y 2323232] [:COLOUR \i]]])))
