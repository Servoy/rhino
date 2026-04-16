package org.mozilla.javascript;

import java.lang.reflect.Method;

// servoy patch, made the bean property its own public class
public final class BeanProperty {
    BeanProperty(String name) {
        this.name = name;
    }

    public BeanProperty(String name, NativeJavaMethod getter) {
        this.name = name;
        this.getter = getter;
        this.setter = null;
    }
    
    public Method getGetter() {
		return getter != null? getter.methods[0].method() : null;
	}

    public Method getSetter() {
		return setter != null? setter.methods[0].method() : null;
	}

    

    final String name;
    NativeJavaMethod getter;
    NativeJavaMethod setter;
}