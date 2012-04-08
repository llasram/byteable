(ns byteable.api
  (:refer-clojure :exclude [read])
  (:require [clojure.string :as str])
  (:import [java.io DataInput DataOutput]))

(defprotocol Byteable
  "Protocol for serializing/deserializing types by a simple protocol similar to
the Hadoop `Writable` interface.  Do *not* directly extend types to this
protocol -- instead use the `extend-byteable` macro."
  (read [this ^DataInput input]
    "Deserialize an object of the type of `this` from `input`.  If `this` is
non-nil and mutable, _may_ destructively modify it to become the deserialized
object.")
  (write [this ^DataOutput output]
    "Serialize `this` to `output`."))

(defmulti read-for
  "Return the function for deserializing instance of `class`."
  (fn [class] class))

(defn byteable?
  "Return true if `class` implements the Byteable protocol."
  [^Class class] (boolean (read-for class)))

(defn- assoc-meta
  [obj key val] (with-meta obj (assoc (meta obj) key val)))

(defn- fixup-meta
  [class [fname [this data :as params] & body]]
  (let [data-class (fname {'read 'java.io.DataInput
                           'write 'java.io.DataOutput})
        ret-class (fname {'read class, 'write nil})
        this (assoc-meta this :tag class)
        data (assoc-meta data :tag data-class)
        params (with-meta [this data]
                 (assoc (meta params) :tag ret-class))]
    (list* fname params body)))

(defn- extend-byteable-one
  [[class & forms]]
  (let [forms (map (partial fixup-meta class) forms)
        [read write] (if (= 'read (ffirst forms)) forms (reverse forms))]
    `(let [read# (fn ~@read), write# (fn ~@write)]
       (extend ~class Byteable {:read read#, :write write#})
       (defmethod read-for ~class [_#] read#))))

(defmacro extend-byteable
  "Make one or more types serializable via the `Byteable` protocol.  Forms
should be as per the arguments to the `extend-protocol` macro following the
protocol."
  [& body] `(do ~@(map extend-byteable-one (partition 3 body))))
