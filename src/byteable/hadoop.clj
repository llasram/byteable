(ns byteable.hadoop
  (:require [clojure.string :as str]
            [byteable.api :as b])
  (:use [shady.defclass :only [defclass]])
  (:import [java.io DataInput DataInputStream DataOutput DataOutputStream]
           [org.apache.hadoop.conf Configured Configuration]
           [org.apache.hadoop.io.serializer
              Deserializer Serializer Serialization]))

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

(defn- byteable-serializer
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

(defn- byteable-deserializer
  [class] (ByteableDeserializer. nil class))


(defn- initialize-bytables
  [^Configuration conf]
  (let [namespaces (.getStrings conf "byteable.serialization.namespaces")]
    (doseq [namespace namespaces]
      (require (symbol namespace)))))

(defclass ByteableSerialization [initialized?]
  :extends Configured

  ByteableSerialization
  (-init [& args] [args (atom nil)])

  Serialization
  (accept [this class]
    (when-not @initialized?
      (when-let [conf (.getConf this)]
        (initialize-bytables conf)
        (reset! initialized? true)))
    (b/byteable? class))

  (getSerializer [this class] (byteable-serializer class))
  (getDeserializer [this class] (byteable-deserializer class)))
