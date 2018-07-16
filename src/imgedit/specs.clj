(ns imgedit.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest]
            [imgedit.implementation :as impl]))

;; utility defs
(def XY-MAX 250)
(defn coord-value-gen
  "Returns a generator for co-ordinate values. Zero-argument version useful for
  with-gen specs; single-argument version limits the generator to 1 to that
  range, useful for combination with gen/bind."
  ([] (coord-value-gen XY-MAX))
  ([n] (gen/large-integer* {:min 1 :max n})))

;; parameter specs
(s/def ::coord-value (s/with-gen (s/and pos-int? #(<= 1 % XY-MAX))
                       #(coord-value-gen)))
(s/def ::x-coord (s/tuple #{:X} ::coord-value))
(s/def ::y-coord (s/tuple #{:Y} ::coord-value))
(s/def ::x-coord-pair (s/tuple #{:XS} ::coord-value ::coord-value))
(s/def ::y-coord-pair (s/tuple #{:YS} ::coord-value ::coord-value))
(s/def ::width (s/tuple #{:WIDTH} ::coord-value))
(s/def ::height (s/tuple #{:HEIGHT} ::coord-value))
(s/def ::colour-value (s/with-gen char? #(gen/char-alpha)))
(s/def ::colour (s/tuple #{:COLOUR} ::colour-value))
(s/def ::param (s/or :width ::width :height ::height
                     :x-coord ::x-coord :y-coord ::y-coord
                     :x-coord-pair ::x-coord-pair :y-coord-pair ::y-coord-pair
                     :colour ::colour))

;; no-arg or indepent arg command specs
(s/def ::new-image (s/tuple #{:NEW} ::width ::height))
(s/def ::clear-image (s/tuple #{:CLEAR}))
(s/def ::show (s/tuple #{:SHOW}))
(s/def ::quit (s/tuple #{:QUIT}))


;; utilities and specs for commands where the arguments should be constrained to
;; the image dimensions (when generating test data)
(def xy-gen
  "A generator returning pairs of numbers in the range 1 to XY-MAX. Useful for
  sharing the same constrained pairs of values between image and command
  generators."
  (gen/tuple (s/gen ::coord-value) (s/gen ::coord-value)))

(defn param-gen
  "Returns a generator for tagged numeric parameters. E.g. (param-gen :X 5))
  returns [:X [1-5]]. Multiple arities for tags with single or double values."
  ([tag value]
   (gen/tuple (gen/return tag) (coord-value-gen value)))
  ([tag value value]
   (gen/tuple (gen/return tag) (coord-value-gen value) (coord-value-gen value))))

(defn pixel-gen
  "Returns a generator for pixel commands. Zero-argument version useful for
  with-gen specs; [x, y] version limits the pixel x,y from 1 to that range,
  useful for combination with gen/bind."
  ([] (pixel-gen XY-MAX XY-MAX))
  ([x y]
   (gen/tuple
     (gen/return :PIXEL) (param-gen :X x) (param-gen :Y y) (s/gen ::colour))))

(s/def ::pixel (s/with-gen (s/tuple #{:PIXEL} ::x-coord ::y-coord ::colour)
                 #(pixel-gen)))

(defn vertical-gen
  "Returns a generator for vertical line commands. Zero-argument version useful
  for with-gen specs; [x, y] version limits the pixel x,y1,y2 from 1 to that
  range, useful for combination with gen/bind. The y1 and y2 values are
  generated independently."
  ([] (vertical-gen XY-MAX XY-MAX))
  ([x y]
   (gen/tuple
     (gen/return :VERTICAL)
     (param-gen :X x)
     (param-gen :YS y y)
     (s/gen ::colour))))

(s/def ::vertical (s/with-gen
                    (s/tuple #{:VERTICAL} ::x-coord ::y-coord-pair ::colour)
                    #(vertical-gen)))

(defn horizontal-gen
  "Returns a generator for horizontal line commands. Zero-argument version useful
  for with-gen specs; [x, y] version limits the pixel x,y1,y2 from 1 to that
  range, useful for combination with gen/bind. The x1 and x2 values are
  generated independently."
  ([] (horizontal-gen XY-MAX XY-MAX))
  ([x y]
   (gen/tuple
     (gen/return :HORIZONTAL)
     (param-gen :XS x x)
     (param-gen :Y y)
     (s/gen ::colour))))

(s/def ::horizontal (s/with-gen
                      (s/tuple #{:HORIZONTAL} ::x-coord-pair ::y-coord ::colour)
                      #(horizontal-gen)))

(defn fill-gen
  "Returns a generator for fill commands. Zero-argument version useful for
  with-gen specs; [x, y] version limits the pixel dimensions from 1 to that
  range, useful for combination with gen/bind."
  ([] (fill-gen XY-MAX XY-MAX))
  ([x y]
   (gen/tuple
     (gen/return :FILL)
     (param-gen :X x)
     (param-gen :Y y)
     (s/gen ::colour))))

(s/def ::fill (s/with-gen
                (s/tuple #{:FILL} ::x-coord ::y-coord ::colour)
                #(fill-gen)))

(s/def ::command (s/tuple #{:COMMAND} ;; TODO: refactor into multi-spec
                          (s/or
                           :new ::new-image
                           :clear ::clear-image
                           :pixel ::pixel
                           :vertical ::vertical
                           :horizontal ::horizontal
                           :fill ::fill
                           :show ::show
                           :quit ::quit)))

;; image specs, checking internal consistency and value types
(s/def :image/width ::coord-value)
(s/def :image/height ::coord-value)
(s/def :image/pixels (s/map-of (s/tuple ::coord-value ::coord-value) char?))

(defn image-gen
  "Given no parameters, generate an image independently (while ensuring pixels are
  contained within image bounds). Given x and y, generate an image of those
  dimensions with pixels within those bounds. Usually paired with xy-gen above
  via a gen/bind combinator, to allow the set of dimensions to be shared with
  command generators."
  ([] (image-gen XY-MAX XY-MAX))
  ([x y]
   (gen/hash-map
     :image/width (gen/return x)
     :image/height (gen/return y)
     :image/pixels (gen/map
                     (gen/tuple (coord-value-gen x) (coord-value-gen y))
                     (s/gen ::colour-value)))))

(s/def ::image
  (s/with-gen
    (s/and
      (s/keys :req [:image/width :image/height :image/pixels])
      impl/every-pixel-in-bounds?)
    #(image-gen)))





;; image-command pair specs, checking command arguments are valid for image.
;; Validation only - see fdef specs below for fdef arg generation
(s/def ::image-command
  (s/and
    (s/cat :image ::image :command ::command)
    (fn [{:keys [image command]}]
      (impl/command-in-bounds? image command))))


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
  :args (s/cat :width ::coord-value :height ::coord-value)
  :ret ::image
  :fn (fn [{:keys [args ret]}]
        (and
          (= (-> ret :image/width) (:width args))
          (= (-> ret :image/height) (:height args)))))

(s/fdef impl/clear
  :args (s/cat :image ::image)
  :ret ::image
  :fn #(= (-> % :ret :image/pixels count) 0))

(s/fdef impl/get-colour
  :args (s/with-gen
          (s/cat :image ::image :x ::x-coord :y ::y-coord)
          #(gen/bind
             xy-gen
             (fn [[x y]]
               (gen/tuple
                 (image-gen x y) (param-gen :X x) (param-gen :Y y)))))
  :ret char?
  :fn #(let [image (-> % :args :image)
              x (-> % :args :x)
              y (-> % :args :y)]
         (and
           (impl/in-bounds? image x)
           (impl/in-bounds? image y))))

(def image-command-pixel
  (gen/bind xy-gen (fn [[x y]] (gen/tuple (image-gen x y) (pixel-gen x y)))))

(s/fdef impl/pixel
  :args (s/with-gen
          (s/cat :image ::image :x ::x-coord :y ::y-coord :c ::colour)
          #(gen/bind
             xy-gen
             (fn [[x y]]
               (gen/tuple
                 (image-gen x y)
                 (param-gen :X x) (param-gen :Y y) (s/gen ::colour)))))
  :ret ::image
  :fn #(let [image (-> % :args :image)
             x (-> % :args :x)
             y (-> % :args :y)]
         (and (impl/in-bounds? image x) (impl/in-bounds? image y))))

(s/fdef impl/vertical-line
  :args (s/with-gen
          (s/cat :image ::image :x ::x-coord :ys ::y-coord-pair :c ::colour)
          #(gen/bind xy-gen
             (fn [[x y]]
               (gen/tuple
                 (image-gen x y)
                 (param-gen :X x) (param-gen :YS y y) (s/gen ::colour)))))
  :ret ::image
  :fn #(let [image (-> % :args :image)
             x (-> % :args :x)
             ys (-> % :args :ys)]
         (and (impl/in-bounds? image x) (impl/in-bounds? image ys))))

(s/fdef impl/horizontal-line
  :args (s/with-gen
          (s/cat :image ::image :xs ::x-coord-pair :y ::y-coord :c ::colour)
          #(gen/bind xy-gen
             (fn [[x y]]
               (gen/tuple
                 (image-gen x y)
                 (param-gen :XS x x) (param-gen :Y y) (s/gen ::colour)))))
  :ret ::image
  :fn #(let [image (-> % :args :image)
             xs (-> % :args :xs)
             y (-> % :args :y)]
         (and (impl/in-bounds? image xs) (impl/in-bounds? image y))))

(s/fdef impl/matching-neighbours
  :args (s/with-gen (s/cat
                      :image ::image
                      :coord (s/tuple ::coord-value ::coord-value)
                      :colour char?)
          #(gen/bind xy-gen
             (fn [[x y]]
               (gen/tuple
                 (image-gen x y)
                 (gen/tuple (coord-value-gen x) (coord-value-gen y))
                 (s/gen ::colour-value)))))
  :ret (s/coll-of (s/tuple ::coord-value ::coord-value) :kind vector)
  :fn #(let [image (-> % :args :image)
             [x y] (-> % :args :coord)]
         (and
           (impl/in-bounds? image [:X x])
           (impl/in-bounds? image [:Y y])
           (<= 0 (count (:ret %)) 4))))  ;; zero to four neighbours found

(s/fdef impl/fill
  :args (s/with-gen
          (s/cat :image ::image :x ::x-coord :y ::y-coord :c ::colour)
          #(gen/bind xy-gen
             (fn [[x y]]
               (gen/tuple
                 (image-gen x y)
                 (param-gen :X x) (param-gen :Y y)
                 (s/gen ::colour)))))
  :ret ::image
  :fn #(let [image (-> % :args :image)
              x (-> % :args :x)
              y (-> % :args :y)]
          (and
            (impl/in-bounds? image x)
            (impl/in-bounds? image y))))

(s/fdef impl/show
  :args (s/cat :image ::image)
  :ret ::image
  :fn #(= (-> % :args :image) (-> % :ret)))
