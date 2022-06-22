(defproject mysql-cluster "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main jepsen.mysql.cluster
  :jvm-opts ["-Djava.awt.headless=true"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.1.18"]
                 [javax.xml.bind/jaxb-api "2.3.0"]
                 [org.clojure/java.jdbc "0.2.2"]
                 [seancorfield/next.jdbc "1.0.445"]
                 [cheshire "5.10.0"]
                 [clj-wallhack "1.0.1"]
                 [mysql/mysql-connector-java "5.1.6"]])