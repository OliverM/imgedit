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

(defn check-dimension [{:keys [image/width image/height]} [dimension value]]
  (case dimension
    :X (<= value width)
    :XS (let [[x1 x2] value] (and (<= x1 width) (<= x2 width)))
    :Y (<= value height)
    :YS (let [[y1 y2] value] (and (<= y1 height) (<= y2 height)))))

(defn new-image
  [width height]
  #:image{:width width :height height :pixels {}})

(defn clear
  [image]
  (assoc image :image/pixels {}))

(defn pixel
  [image [_ x :as x-coord] [_ y :as y-coord] c]
  (assoc-in image [:image/pixels [x y]] c))

(defn vertical-line
  [image [_ x :as x-coord] [_ y1 y2 :as y-coord-pair] c]
  (let [pixels (for [y (range (min y1 y2) (inc (max y1 y2)))]
                 [[:X x] [:Y y] c])]
    (reduce #(apply pixel %1 %2) image pixels)))

(defn horizontal-line
  [image [_ x1 x2 :as x-coord-pair] [_ y :as y-coord] c]
  (let [pixels (for [x (range (min x1 x2) (inc (max x1 x2)))]
                 [[:X x] [:Y y] c])]
    (reduce #(apply pixel %1 %2) image pixels)))

(defn fill
  [image x-coord y-coord c]
  image)

(defn show
  "Meets the spec of a default colour of 'O' by using it as the not-found value
  for get (when no pixels have been written to that [x y] position)."
  [{:keys [image/width image/height image/pixels] :as image}]
  (let [image-lines (partition width
                      (for [y (range 1 (inc height))
                            x (range 1 (inc width))]
                        (get pixels [x y] \O)))]
    (doseq [line image-lines] (run! print line) (newline)))
  image)

(defn quit
  [image]
  image)

;; you can't spec defmethods as fully as functions, so here I'm only using the
;; defmulti command* to invoke the correct command function based on the command
;; tag
(defmulti command* (fn [_ [_ [command-tag _]]] command-tag))
(defmethod command* :NEW
  [image [_ [_ [_ w] [_ h]]]] (new-image w h))
(defmethod command* :CLEAR
  [image command] (clear image))
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
  [image command] (show image))
(defmethod command* :QUIT
  [image command] (quit image))

(defn pump
  "Handle user input and maintain state. Each pump invocation accepts and returns
  an image, either unchanged if the input was erroneous or with the command
  applied if not."
  [image]
  (print "> ")
  (let [command (param-transform (command-parser (read-line)))]
    (if (s/valid? ::command command)
      (recur (command* image command))
      (do
        (s/explain ::command command)
        (recur image)))))

(defn -main
  "Main entry point. Launch the interactive session with a default blank image."
  [& args]
  (pump (new-image 10 10)))
