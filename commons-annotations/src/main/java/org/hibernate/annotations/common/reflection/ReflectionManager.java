package org.hibernate.annotations.common.reflection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * The entry point to the reflection layer (a.k.a. the X* layer).
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
public interface ReflectionManager {

	public <T> XClass toXClass(Class<T> clazz);

	public Class toClass(XClass xClazz);

	public Method toMethod(XMethod method);

	public <T> XClass classForName(String name, Class<T> caller) throws ClassNotFoundException;

	public XPackage packageForName(String packageName) throws ClassNotFoundException;

	public <T> boolean equals(XClass class1, Class<T> class2);

    public AnnotationReader buildAnnotationReader(AnnotatedElement annotatedElement);

    public Map getDefaults();
}
