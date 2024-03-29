(ns byteable.core
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
  "Return the function for deserializing instances of `class`."
  identity)

(defmethod read-for :default [_] nil)

(defmulti write-for
  "Return the function for serializing instances of `class`."
  identity)

(defmethod write-for :default [_] nil)

(defn byteable?
  "Return true if `class` has been made byteable."
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
        build-fmap #(assoc %1 (-> %2 first keyword) (list* 'fn %2))
        fmap (reduce build-fmap {} forms)]
    `(let [fmap# ~fmap, {read# :read, write# :write} fmap#]
       (extend ~class Byteable fmap#)
       (defmethod read-for ~class [_#] read#)
       (defmethod write-for ~class [_#] write#))))

(defmacro extend-byteable
  "Make one or more types serializable via the `Byteable` protocol.  Forms
should be as per the arguments to the `extend-protocol` macro following the
protocol."
  [& body] `(do ~@(map extend-byteable-one (partition 3 body))))
