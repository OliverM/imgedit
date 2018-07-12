(ns imgedit.core
  (:require [instaparse.core :as insta]
            [clojure.spec.alpha :as s])
  (:gen-class))

(def command-parser
  (insta/parser "
     S = COMMAND
     <COMMAND> = NEW | CLEAR | PIXEL | VERTICAL | HORIZONTAL | FILL | SHOW | QUIT
     NEW = <'I'> <ws> WIDTH <ws> HEIGHT
     CLEAR = <'C'> <ws> XY
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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
