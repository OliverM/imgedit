(ns imgedit.specs
  (:require [clojure.spec.alpha :as s]
            [imgedit.implementation :as impl]))

;; parameter specs
(def XY-MAX 250)
(s/def ::coord-value (s/and pos-int? #(<= 1 % XY-MAX)))
(s/def ::x-coord (s/tuple #{:X} ::coord-value))
(s/def ::y-coord (s/tuple #{:Y} ::coord-value))
(s/def ::x-coord-pair (s/tuple #{:XS} ::coord-value ::coord-value))
(s/def ::y-coord-pair (s/tuple #{:YS} ::coord-value ::coord-value))
(s/def ::width (s/tuple #{:WIDTH} ::coord-value))
(s/def ::height (s/tuple #{:HEIGHT} ::coord-value))
(s/def ::colour-value char?)
(s/def ::colour (s/tuple #{:COLOUR} ::colour-value))
(s/def ::param (s/or :width ::width :height ::height
                     :x-coord ::x-coord :y-coord ::y-coord
                     :x-coord-pair ::x-coord-pair :y-coord-pair ::y-coord-pair
                     :colour ::colour))

;; command specs
(s/def ::new-image (s/tuple #{:NEW} ::width ::height))
(s/def ::clear-image (s/tuple #{:CLEAR}))
(s/def ::pixel (s/tuple #{:PIXEL} ::x-coord ::y-coord ::colour))
(s/def ::vertical (s/tuple #{:VERTICAL} ::x-coord ::y-coord-pair ::colour))
(s/def ::horizontal (s/tuple #{:HORIZONTAL} ::x-coord-pair ::y-coord ::colour))
(s/def ::fill (s/tuple #{:FILL} ::x-coord ::y-coord ::colour))
(s/def ::show (s/tuple #{:SHOW}))
(s/def ::quit (s/tuple #{:QUIT}))
(s/def ::command (s/tuple #{:COMMAND}
                          (s/or
                           :new ::new-image
                           :clear ::clear-image
                           :pixel ::pixel
                           :vertical ::vertical
                           :horizontal ::horizontal
                           :fill ::fill
                           :show ::show
                           :quit ::quit)))

;; image specs
(s/def :image/width ::coord-value)
(s/def :image/height ::coord-value)
(s/def :image/pixels (s/map-of (s/tuple ::coord-value ::coord-value) char?))
(s/def ::image (s/and
                 (s/keys :req [:image/width :image/height :image/pixels])
                 impl/every-pixel-in-bounds?))




;; you can't spec defmethods as fully as functions, so here I'm only using the
;; defmulti command* to invoke the correct command function based on the command
;; tag
(defmulti command* (fn [_ [_ [command-tag _]]] command-tag))
(defmethod command* :NEW
  [image [_ [_ [_ w] [_ h]]]] (impl/new-image w h))
(defmethod command* :CLEAR
  [image _] (impl/clear image))
(defmethod command* :PIXEL
  [image [_ [_ x-coord y-coord colour]]]
  (impl/pixel image x-coord y-coord colour))
(defmethod command* :VERTICAL
  [image [_ [_ x-coord y-coord-pair colour]]]
  (impl/vertical-line image x-coord y-coord-pair colour))
(defmethod command* :HORIZONTAL
  [image [_ [_ x-coord-pair y-coord colour]]]
  (impl/horizontal-line image x-coord-pair y-coord colour))
(defmethod command* :FILL
  [image [_ [_ x-coord y-coord colour]]]
  (impl/fill image x-coord y-coord colour))
(defmethod command* :SHOW
  [image _] (impl/show image))
(defmethod command* :QUIT
  [_ _] (impl/quit))


(s/fdef impl/new-image
  :args (s/cat :width pos-int? :height pos-int?)
  :ret ::image
  :fn #(and
         (= (:image/width :ret) :width)
         (= (:image/height :ret) :height)))

(s/fdef impl/clear
  :args (s/cat :image ::image )
  :ret ::image
  :fn #(= (:image/pixels :ret) {}))

(s/fdef impl/get-colour
  :args (s/cat :image ::image :x ::x-coord :y ::y-coord)
  :ret char?
  :fn #(and
         (impl/in-bounds? :image :x)
         (impl/in-bounds? :image :y)))

(s/fdef impl/pixel
  :args (s/cat :image ::image :x ::x-coord :y ::y-coord :c ::colour)
  :ret ::image
  :fn #(and
         (impl/in-bounds? :image :x)
         (impl/in-bounds? :image :y)))

(s/fdef impl/vertical-line
  :args (s/cat :image ::image :x ::x-coord :ys ::y-coord-pair :c ::colour)
  :ret ::image
  :fn #(and
         (impl/in-bounds? :image :x)
         (impl/in-bounds? :image :ys)))

(s/fdef impl/horizontal-line
  :args (s/cat :image ::image :xs ::x-coord-pair :y ::y-coord :c ::colour)
  :ret ::image
  :fn #(and
         (impl/in-bounds? :image :xs)
         (impl/in-bounds? :image :y)))

(s/fdef impl/matching-neighbours
  :args (s/cat
          :image ::image
          :coord (s/tuple :x pos-int? :y pos-int?)
          :colour char?)
  :ret (s/coll-of (s/tuple :dx pos-int? :dy pos-int?) :kind vector)
  :fn #(and
         (and
           (impl/in-bounds? :image [:X :x])
           (impl/in-bounds? :image [:Y :y])
           (<= 0 (count :ret) 4))))  ;; zero to four neighbours found

(s/fdef impl/fill
  :args (s/cat :image ::image :x ::x-coord :y ::y-coord :c ::colour)
  :ret ::image
  :fn #(and
         (impl/in-bounds? :image :x)
         (impl/in-bounds? :image :y)))

(s/fdef impl/show
  :args (s/cat :image ::image)
  :ret ::image)
