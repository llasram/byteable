(defproject byteable "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :profiles
    {:dev {:dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]]
           :warn-on-reflection true}
     :release {:dependencies []}})
