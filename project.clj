(defproject imgedit "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [instaparse "1.4.9"]]
  :main ^:skip-aot imgedit.core
  :monkeypatch-clojure-test false ;; allows lein test integration with test.check
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}
             :uberjar {:aot :all}})
