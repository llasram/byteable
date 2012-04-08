package byteable.hadoop;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;
import org.apache.hadoop.io.serializer.Deserializer;

class ByteableSerialization
    extends Configured
    implements Serialization {

private static final String NS = "byteable.hadoop";

static {
    RT.var("clojure.core", "require").invoke(Symbol.intern(NS));
}

private static final Var initialize = RT.var(NS, "initialize-byteables");
private static final Var serializer = RT.var(NS, "byteable-serializer");
private static final Var deserializer = RT.var(NS, "byteable-deserializer");
private static final Var byteable_p = RT.var("byteable.core", "byteable?");

private boolean initialized = false;

public boolean
accept(Class c) {
    if (!initialized) {
        initialize.invoke(getConf());
        initialized = true;
    }
    return RT.booleanCast(byteable_p.invoke(c));
}

public Serializer
getSerializer(Class c) {
    return (Serializer)serializer.invoke(c);
}

public Deserializer
getDeserializer(Class c) {
    return (Deserializer)deserializer.invoke(c);
}

}
