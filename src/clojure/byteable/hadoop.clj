(ns byteable.hadoop
  (:require [clojure.string :as str]
            [byteable.api :as b])
  (:import [java.io DataInput DataInputStream DataOutput DataOutputStream]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.io.serializer Deserializer Serializer]
           [byteable.hadoop ByteableSerialization]))

(deftype ByteableSerializer [^:unsynchronized-mutable output]
  Serializer
  (open [this ostream]
    (set! output (if (instance? DataOutputStream ostream)
                   ostream
                   (DataOutputStream. ostream))))
  (serialize [this byteable]
    (b/write byteable output))
  (close [this]
    (when-let [output output]
      (.close ^DataOutputStream output))))

(defn byteable-serializer
  [class] (ByteableSerializer. nil))

(deftype ByteableDeserializer [^:unsynchronized-mutable input, class]
  Deserializer
  (open [this istream]
    (set! input (if (instance? DataInputStream istream)
                  istream
                  (DataInputStream. istream))))
  (deserialize [this byteable]
    (if (nil? byteable)
      ((b/read-for class) nil input)
      (b/read byteable input)))
  (close [this]
    (when-let [input input]
      (.close ^DataInputStream input))))

(defn byteable-deserializer
  [class] (ByteableDeserializer. nil class))

(defn initialize-byteables
  [^Configuration conf]
  (let [namespaces (.getStrings conf "byteable.serialization.namespaces")]
    (doseq [namespace namespaces]
      (require (symbol namespace)))))
