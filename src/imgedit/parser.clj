(ns imgedit.parser
  (:require [instaparse.core :as insta]))

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

(defn parse
  [input-string]
  (-> input-string
    command-parser
    param-transform))

