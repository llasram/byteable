(ns byteable.hadoop-test
  (:use clojure.test)
  (:require [byteable [api :as b] [hadoop]])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream
                    DataInput DataOutput]
           [byteable.hadoop ByteableSerialization]
           [org.apache.hadoop.mapred JobConf]
           [org.apache.hadoop.io.serializer SerializationFactory]
           [java.net InetAddress]))

(b/extend-byteable
  InetAddress
  (read [_ input]
    (let [len (int (.readByte input)),
          bytes (byte-array len)]
      (.readFully input bytes)
      (InetAddress/getByAddress bytes)))
  (write [addr output]
    (let [bytes (.getAddress addr)]
      (doto output
        (.writeByte (byte (alength bytes)))
        (.write bytes)))))

(defn- serialize [^SerializationFactory hsf obj]
  (let [baos (ByteArrayOutputStream.)]
    (with-open [ser (doto (.getSerializer hsf (class obj))
                      (.open baos))]
      (.serialize ser obj))
    (.toByteArray baos)))

(defn- deserialize [^SerializationFactory hsf class bytes]
  (let [bais (ByteArrayInputStream. bytes)]
    (with-open [deser (doto (.getDeserializer hsf class)
                        (.open bais))]
      (.deserialize deser nil))))

(deftest test-hadoop
  (testing "Ensure Hadoop Byteable serialization round-trips"
    (let [conf (doto (JobConf.)
                 (.set "io.serializations"
                       (.getName ByteableSerialization)))
          hsf (SerializationFactory. conf)
          addr1 (InetAddress/getByName "192.168.0.1")
          bytes (serialize hsf addr1)
          addr2 (deserialize hsf InetAddress bytes)]
      (is (= addr1 addr2)))))
