(ns imgedit.core
  (:require [instaparse.core :as insta]
            [clojure.spec.alpha :as s])
  (:gen-class))

(def command-parser
  "Generate a parser for image editor commands."
  (insta/parser "
     S = COMMAND
     <COMMAND> = NEW | CLEAR | PIXEL | VERTICAL | HORIZONTAL | FILL | SHOW | QUIT
     NEW = <'I'> <ws> WIDTH <ws> HEIGHT
     CLEAR = <'C'>
     PIXEL = <'L'> <ws> XYC
     VERTICAL = <'V'> <ws> SEGMENT
     HORIZONTAL = <'H'> <ws> SEGMENT
     <SEGMENT> = U <ws> V1 <ws> V2 <ws> COLOUR
     FILL = <'F'> <ws> XYC
     SHOW = <'S'>
     QUIT = <'X'>
     <XYC> = XY <ws> COLOUR
     <XY> = X <ws> Y
     COLOUR = #'[A-Z]'
     X = coord
     Y = coord
     U = coord
     V1 = coord
     V2 = coord
     WIDTH = coord
     HEIGHT = coord
     <coord> = #'[1-9][0-9]*'
     ws = #'\\s+'
     "))

(defn param-transform
  "Given a parse-tree from command-parser, above, convert the :coord terminals to
  numbers."
  [parsed-command]
  (letfn [(reader [tag v] (vector tag (clojure.edn/read-string v)))]
    (insta/transform
      {:X #(reader :X %)
       :Y #(reader :Y %)
       :U #(reader :U %)
       :V1 #(reader :V1 %)
       :V2 #(reader :V2 %)
       :WIDTH #(reader :WIDTH %)
       :HEIGHT #(reader :HEIGHT %)}
      parsed-command)))

(def XY-MAX 250)
(s/def ::coord-value (s/and int? #(< 1 % XY-MAX)))
(s/def ::coord-name #{:X :Y :U :V1 :V2})
(s/def ::coord (s/tuple ::coord-name ::coord-value))
(s/def ::dimension-name #{:WIDTH :HEIGHT})
(s/def ::dimension (s/tuple ::dimension-name ::coord-value))
(s/def ::colour-value (s/and string? #(re-matches #"[A-Z]" %)))
(s/def ::colour (s/tuple #{:COLOUR} ::colour-value))
(s/def ::param (s/or :dimension ::dimension :coord ::coord :colour ::colour))

(s/def ::new-image (s/tuple #{:NEW} ::dimension ::dimension))
(s/def ::clear-image (s/tuple #{:CLEAR}))
(s/def ::pixel (s/tuple #{:PIXEL} ::coord ::coord ::colour))
(s/def ::vertical (s/tuple #{:VERTICAL} ::coord ::coord ::coord ::colour))
(s/def ::horizontal (s/tuple #{:HORIZONTAL} ::coord ::coord ::coord ::colour))
(s/def ::fill (s/tuple #{:FILL} ::coord ::coord ::colour))
(s/def ::show (s/tuple #{:SHOW}))
(s/def ::quit (s/tuple #{:QUIT}))

(s/def ::command (s/or
                   :new ::new-image
                   :clear ::clear-image
                   :pixel ::pixel
                   :vertical ::vertical
                   :horizontal ::horizontal
                   :fill ::fill
                   :show ::show
                   :quit ::quit))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
