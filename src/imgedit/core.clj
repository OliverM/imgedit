(ns imgedit.core
  (:require [instaparse.core :as insta])
  (:gen-class))

(def command-parser
  (insta/parser "
     S = COMMAND
     COMMAND = NEW | CLEAR | PIXEL | VERTICAL | HORIZONTAL | FILL | SHOW | QUIT
     NEW = 'I' <ws> WIDTH <ws> HEIGHT
     CLEAR = 'C' <ws> XY
     PIXEL = 'L' <ws> XYC
     SEGMENT = DIRECTION <ws> X <ws> Y <ws> Y2 <ws> COLOUR
     DIRECTION = VERTICAL | HORIZONTAL
     VERTICAL = 'V'
     HORIZONTAL = 'H'
     FILL = 'F' <ws> XYC
     SHOW = 'S'
     QUIT = 'X'
     XYC = XY <ws> COLOUR
     XY = X <ws> Y
     COLOUR = #'[A-Z]'
     X = number
     Y = number
     Y2 = number
     WIDTH = number
     HEIGHT = number
     number = #'[0-9]+'
     ws = #'\\s+'
     "))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
