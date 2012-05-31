(ns byteable.hadoop
  (:require [clojure.string :as str]
            [byteable.core :as b])
  (:import [java.io DataInput DataInputStream DataOutput DataOutputStream]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.io.serializer Deserializer Serializer]
           [byteable.hadoop ByteableSerialization]))

(deftype ByteableSerializer [^:unsynchronized-mutable output, bwrite]
  Serializer
  (open [this ostream]
    (set! output (if (instance? DataOutputStream ostream)
                   ostream
                   (DataOutputStream. ostream))))
  (serialize [this byteable]
    (bwrite byteable output))
  (close [this]
    (when-let [output output]
      (.close ^DataOutputStream output))))

(defn byteable-serializer
  [class] (ByteableSerializer. nil (b/write-for class)))

(deftype ByteableDeserializer [^:unsynchronized-mutable input, bread]
  Deserializer
  (open [this istream]
    (set! input (if (instance? DataInputStream istream)
                  istream
                  (DataInputStream. istream))))
  (deserialize [this byteable]
    (if (nil? byteable)
      (bread nil input)
      (bread byteable input)))
  (close [this]
    (when-let [input input]
      (.close ^DataInputStream input))))

(defn byteable-deserializer
  [class] (ByteableDeserializer. nil (b/read-for class)))

(defn initialize-byteables
  [^Configuration conf]
  (let [namespaces (.getStrings conf "byteable.serialization.namespaces")]
    (doseq [namespace namespaces]
      (require (symbol namespace)))))
