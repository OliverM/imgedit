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

(defmulti command #(-> % second first))
(defmethod command :NEW
  [[_ [_ [_ width] [_ height]]]]
  )
(defmethod command :CLEAR
  [_])
(defmethod command :HORIZONTAL
  [[_ [_]]])
(defmethod command :PIXEL
  [[_ [_]]])
(defmethod command :VERTICAL
  [[_ [_]]])
(defmethod command :FILL
  [[_ [_]]])
(defmethod command :SHOW
  [_])
(defmethod command :QUIT
  [_])


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
