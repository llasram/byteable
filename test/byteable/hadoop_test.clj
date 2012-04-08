(ns byteable.hadoop-test
  (:use clojure.test)
  (:require [byteable.core :as b])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream
                    DataInput DataOutput]
           [byteable.hadoop ByteableSerialization]
           [org.apache.hadoop.mapred JobConf]
           [org.apache.hadoop.io IntWritable]
           [org.apache.hadoop.io.serializer
              SerializationFactory WritableSerialization]
           [java.net InetAddress]))

(b/extend-byteable
  InetAddress
  (read [_ input]
    (let [bytes (-> input .readByte byte-array)]
      (.readFully input bytes)
      (InetAddress/getByAddress bytes)))
  (write [addr output]
    (let [bytes (.getAddress addr)]
      (.writeByte output (byte (alength bytes)))
      (.write output bytes))))

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
  (let [conf (doto (JobConf.)
               (.set "io.serializations"
                     (str (.getName ByteableSerialization) ","
                          (.getName WritableSerialization))))
        hsf (SerializationFactory. conf)]
    (testing "ensure Hadoop Writable serialization round-trips"
      (let [int1 (IntWritable. 31337)
            bytes (serialize hsf int1)
            int2 (deserialize hsf IntWritable bytes)]
        (is (= int1 int2))))
    (testing "Ensure Hadoop Byteable serialization round-trips"
      (let [addr1 (InetAddress/getByName "192.168.0.1")
            bytes (serialize hsf addr1)
            addr2 (deserialize hsf InetAddress bytes)]
        (is (= addr1 addr2))))))
