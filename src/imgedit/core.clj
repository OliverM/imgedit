(ns imgedit.core
  "The core namespace only acts as a host for the -main handler, which invokes the
  pump function, the main application loop. Individual commands are parsed in
  the parser namespace, specs are defined in the specs namespace, and the
  application functionality is implemented in the implementation namespace."
  (:require [clojure.spec.alpha :as s]
            [imgedit.parser :as p]
            [imgedit.implementation :as impl]
            [imgedit.specs :as is])
  (:gen-class))

(defn pump
  "Handle user input and maintain state. Each pump invocation accepts and returns
  an image, either unchanged if the input was erroneous or with the command
  applied if not."
  [image]
  (print "> ")
  (flush)
  (let [command (p/parse (read-line))]
    (if (s/valid? ::is/command command)
      (recur (is/command* image command))
      (do
        (s/explain ::is/command command)
        (recur image)))))

(defn -main
  "Main entry point. Launch the interactive session with a default blank image."
  [& args]
  (println "OnTheMarket Test, by Oliver Mooney (oliver.mooney@gmail.com)")
  (pump (impl/new-image 10 10)))
