# byteable

Byteable is a Clojure protocol-based serialization interface for Hadoop.  It is
primarily intended to complement the Hadoop `Writable` serialization interface,
but may be useful in other contexts as well.

## Installation

Byteable is in Clojars.  Add this `:dependency` to your
[Leiningen](https://github.com/technomancy/leiningen) `project.clj`:

```clj
[byteable "0.1.0-SNAPSHOT"]
```

## Usage

The Hadoop `Writable` interface is nice in that it allows for compact,
efficient serialization, but requiring types to implement the `Writable`
interface in order to be serializable is a very limiting constraint.
`Byteable` provides the same low-level abstract interface, but as a Clojure
protocol + multimethod, allowing independent implementation of the interface
for arbitrary types.

### Extending to types

Use the `byteable.core/extend-byteable` macro to implement the low-level `read`
and `write` de/serialization functions for a particular type.  The
`extend-byteable` macro will automatically annotate parameters with the
appropriate type hints.  For example:

```clj
(ns example.byteable
  (:require [byteable.core :as b]))

(b/extend-byteable
  InetAddress
  (read [_ input]
    (let [len (int (.readByte input)), 
          bytes (byte-array len)]
      (.readFully input bytes)
      (InetAddress/getByAddress bytes)))
  (write [addr output]
    (let [bytes (.getAddress addr)]
      (.writeByte output (byte (alength bytes)))
      (.write output bytes))))
```

### Enabling in Hadoop

In your Hadoop configuration, include `byteable.hadoop.ByteableSerialization`
in your list of `io.serializations`.  Set `byteable.serialization.namespaces`
to a comma-separated list of namespaces to `require` in order to load byteable
serialization implementations.  For example:

```xml
<configuration>
  <property>
    <name>io.serializations</name>
    <value>
      byteable.hadoop.ByteableSerialization,
      org.apache.hadoop.io.serializer.WritableSerialization
    </value>
  </property>

  <property>
    <name>byteable.serialization.namespaces</name>
    <value>example.byteable</value>
  </property>
</configuration>
```

## License

Copyright © 2012 Marshall T. Vandegrift

Distributed under the Eclipse Public License, the same as Clojure.
