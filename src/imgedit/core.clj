(ns imgedit.core
  (:require [instaparse.core :as insta]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen])
  (:gen-class))

(def command-parser
  "Generate a parser for image editor commands."
  (insta/parser "
     COMMAND = NEW | CLEAR | PIXEL | VERTICAL | HORIZONTAL | FILL | SHOW | QUIT
     NEW = <'I'> <ws> WIDTH <ws> HEIGHT
     CLEAR = <'C'>
     PIXEL = <'L'> <ws> XYC
     VERTICAL = <'V'> <ws> X <ws> YS <ws> COLOUR
     HORIZONTAL = <'H'> <ws> XS <ws> Y <ws> COLOUR
     FILL = <'F'> <ws> XYC
     SHOW = <'S'>
     QUIT = <'X'>
     <XYC> = XY <ws> COLOUR
     <XY> = X <ws> Y
     XS = PAIR
     YS = PAIR
     <PAIR> = V1 <ws> V2
     COLOUR = #'[A-Z]'
     X = coord
     Y = coord
     <V1> = coord
     <V2> = coord
     WIDTH = coord
     HEIGHT = coord
     <coord> = #'[1-9][0-9]*'
     ws = #'\\s+'
     "))

(defn param-transform
  "Given a parse-tree from command-parser, above, convert the :coord terminals to
  numbers."
  [parsed-command]
  (letfn [(reader [v] (clojure.edn/read-string v))
          (single-tag [tag v] (vector tag (reader v)))
          (double-tag [tag v1 v2] (vector tag (reader v1) (reader v2)))]
    (insta/transform
      {:X #(single-tag :X %)
       :Y #(single-tag :Y %)
       :XS #(double-tag :XS %1 %2)
       :YS #(double-tag :YS %1 %2)
       :WIDTH #(single-tag :WIDTH %)
       :HEIGHT #(single-tag :HEIGHT %)
       :COLOUR #(vector :COLOUR (first %))}
      parsed-command)))

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
(s/def ::image (s/keys :req [:image/width :image/height :image/pixels]))

(defn- in-bounds?
  "Is the supplied dimension in bounds of the supplied image?"
  [{:keys [image/width image/height]} [dimension value]]
  (case dimension
    :X (<= value width)
    :XS (let [[x1 x2] value] (and (<= x1 width) (<= x2 width)))
    :Y (<= value height)
    :YS (let [[y1 y2] value] (and (<= y1 height) (<= y2 height)))))

(defn new-image
  "Create a new image from the supplied dimensions. Uses a sparse map
  representation of pixels."
  [width height]
  #:image{:width width :height height :pixels {}})

(s/fdef new-image
  :args (s/cat :width pos-int? :height pos-int?)
  :ret ::image
  :fn #(and
         (= (:image/width :ret) :width)
         (= (:image/height :ret) :height)))

(defn clear
  "Given an image, returns an image with the pixel information removed."
  [image]
  (assoc image :image/pixels {}))

(s/fdef clear
  :args (s/cat :image ::image )
  :ret ::image
  :fn #(= (:image/pixels :ret) {}))

(defn- get-colour
  "Return the colour at the supplied co-ordinates in the supplied image. Returns
  the default value of 'O' if no pixel has been stored at that location so far."
  [{:keys [image/pixels]} [_ x] [_ y]]
  (get pixels [x y] \O))

(defn pixel
  "Given an image and a set of pixel dimensions, returns an image with that pixel
  added to the image. This is the main means of adding pixel information to an
  image; other pixel-adding functions ultimately invoke this function."
  [image [_ x] [_ y] [_ c]]
  (assoc-in image [:image/pixels [x y]] c))

(s/fdef pixel
  :args (s/cat :image ::image :x ::x-coord :y ::y-coord :c ::colour)
  :ret ::image
  :fn #(and
         (in-bounds? :image :x)
         (in-bounds? :image :y)))

(defn vertical-line
  "Given an image and a set of vertical line dimensions, returns an image with
  that line added to the image."
  [image [_ x] [_ y1 y2] c]
  (let [pixels (for [y (range (min y1 y2) (inc (max y1 y2)))]
                 [[:X x] [:Y y] c])]
    (reduce #(apply pixel %1 %2) image pixels)))

(s/fdef vertical-line
  :args (s/cat :image ::image :x ::x-coord :ys ::y-coord-pair :c ::colour)
  :ret ::image
  :fn #(and
         (in-bounds? :image :x)
         (in-bounds? :image :ys)))

(defn horizontal-line
  "Given an image and a set of horizontal line dimensions, returns an image with
  that line added to the image."
  [image [_ x1 x2] [_ y] c]
  (let [pixels (for [x (range (min x1 x2) (inc (max x1 x2)))]
                 [[:X x] [:Y y] c])]
    (reduce #(apply pixel %1 %2) image pixels)))

(s/fdef vertical-line
  :args (s/cat :image ::image :xs ::x-coord-pair :y ::y-coord :c ::colour)
  :ret ::image
  :fn #(and
         (in-bounds? :image :xs)
         (in-bounds? :image :y)))

(defn- matching-neighbours
  "Given an image, an [x, y] co-ordinate and a colour, return the neighbours of
  that point matching that colour. Neighbours are contiguous (they share a
  side). When a pixel is not present in the sparse pixel representation the
  default colour of 'O' is used."
  [{:keys [image/width image/height] :as image} [x y] c]
  (for [dx [(dec x) x (inc x)]
        dy [(dec y) y (inc y)]
        :when (and
                (>= dx 1) ;; check candidate in bounds of image...
                (>= dy 1)
                (<= dx width)
                (<= dy height)
                (or (= x dx) (= y dy)) ;; and not corner pixel to pixel
                (not (and (= x dx) (= y dy))) ;; and not same pixel
                (= c (get-colour image [:X dx] [:Y dy])))] ;; and has same colour
    [dx dy]))

(s/fdef matching-neighbours
  :args (s/cat
          :image ::image
          :coord (s/tuple :x pos-int? :y pos-int?)
          :colour char?)
  :ret (s/coll-of (s/tuple :dx pos-int? :dy pos-int?) :kind vector)
  :fn #(and
         (and
           (in-bounds? :image [:X :x])
           (in-bounds? :image [:Y :y])
           (<= 0 (count :ret) 4)))) ;; zero to four neighbours found

(defn fill [image [_ x :as x-coord] [_ y :as y-coord] c]
  "Given an image, pixel co-ordinates and a colour, colours the pixel at that
  location that colour, as well as all contiguous pixels sharing the original
  pixel's colour. Contiguous pixels share sides (i.e. diagonally-touching pixels
  are excluded). When no pixel at the queried location is found, the default
  colour of 'O' is used."
  (let [source-colour (get-colour image x-coord y-coord)]
    (loop [image image
           candidates [[x y]]]
      (if (empty? candidates)
        image
        (let [[cx cy] (first candidates)
              updated-image (pixel image [:X cx] [:Y cy] c)]
          (recur
            updated-image
            (into (next candidates)
              (matching-neighbours updated-image [cx cy] source-colour))))))))

(s/fdef fill
  :args (s/cat :image ::image :x ::x-coord :y ::y-coord :c ::colour)
  :ret ::image
  :fn #(and
         (in-bounds? :image :x)
         (in-bounds? :image :y)))

(defn show
  "Display an image via stdout. Transforms the sparse map representation of pixels
  into an array of characters suitable for display, printing it by side-effect.
  When no pixel data is found at a location, the default colour of 'O' is used.
  Returns the image."
  [{:keys [image/width image/height] :as image}]
  (let [image-lines (partition width
                      (for [y (range 1 (inc height))
                            x (range 1 (inc width))]
                        (get-colour image [:X x] [:Y y])))]
    (doseq [line image-lines] (run! print line) (newline)))
  image)

(s/fdef show
  :args (s/cat :image ::image)
  :ret ::image)

(defn quit
  []
  (System/exit 0))

;; you can't spec defmethods as fully as functions, so here I'm only using the
;; defmulti command* to invoke the correct command function based on the command
;; tag
(defmulti command* (fn [_ [_ [command-tag _]]] command-tag))
(defmethod command* :NEW
  [image [_ [_ [_ w] [_ h]]]] (new-image w h))
(defmethod command* :CLEAR
  [image _] (clear image))
(defmethod command* :PIXEL
  [image [_ [_ x-coord y-coord colour]]]
  (pixel image x-coord y-coord colour))
(defmethod command* :VERTICAL
  [image [_ [_ x-coord y-coord-pair colour]]]
  (vertical-line image x-coord y-coord-pair colour))
(defmethod command* :HORIZONTAL
  [image [_ [_ x-coord-pair y-coord colour]]]
  (horizontal-line image x-coord-pair y-coord colour))
(defmethod command* :FILL
  [image [_ [_ x-coord y-coord colour]]]
  (fill image x-coord y-coord colour))
(defmethod command* :SHOW
  [image _] (show image))
(defmethod command* :QUIT
  [_ _] (quit))

(defn pump
  "Handle user input and maintain state. Each pump invocation accepts and returns
  an image, either unchanged if the input was erroneous or with the command
  applied if not."
  [image]
  (print "> ")
  (flush)
  (let [command (param-transform (command-parser (read-line)))]
    (if (s/valid? ::command command)
      (recur (command* image command))
      (do
        (s/explain ::command command)
        (recur image)))))

(defn -main
  "Main entry point. Launch the interactive session with a default blank image."
  [& args]
  (println "OnThe Market Test, by Oliver Mooney (oliver.mooney@gmail.com)")
  (pump (new-image 10 10)))
