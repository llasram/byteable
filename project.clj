(defproject byteable "0.2.0"
  :description "Clojure protocol-based serialization interface for Hadoop."
  :url "https://github.com/llasram/byteable"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]]
                   :warn-on-reflection true}})

(ns leiningen.release
  (:require [leiningen.core.eval :as eval]
            [leiningen.jar :as jar]
            [leiningen.pom :as pom]
            [leiningen.with-profile :as wp]))
(defn release [project]
  (eval/prep project)
  (let [project-m (meta project)
        project-wp (-> project-m (:without-profiles project) (dissoc :prep-tasks))
        project (with-meta (dissoc project :prep-tasks)
                  (assoc project-m :without-profiles project-wp))]
    ;; Potentially a bug in lein, but :user profile dependencies end up in POMs
    ;; unless explicitly run tasks with only the "default" profile
    (wp/with-profile project "default" "pom")
    (wp/with-profile project "default" "jar")))
