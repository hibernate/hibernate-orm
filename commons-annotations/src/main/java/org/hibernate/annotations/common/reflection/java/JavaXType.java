//$Id: PropertyTypeExtractor.java 9316 2006-02-22 20:47:31Z epbernard $
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.Type;
import java.util.Collection;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeUtils;

/**
 * The Java X-layer equivalent to a Java <code>Type</code>.
 *
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
abstract class JavaXType {

	private final TypeEnvironment context;
	private final JavaReflectionManager factory;
	private final Type approximatedType;
	private final Type boundType;

	protected JavaXType(Type unboundType, TypeEnvironment context, JavaReflectionManager factory) {
		this.context = context;
		this.factory = factory;
		this.boundType = context.bind( unboundType );
		this.approximatedType = factory.toApproximatingEnvironment( context ).bind( unboundType );
	}

	abstract public boolean isArray();

	abstract public boolean isCollection();

	abstract public XClass getElementClass();

	abstract public XClass getClassOrElementClass();

	abstract public Class<? extends Collection> getCollectionClass();

	abstract public XClass getMapKey();

	abstract public XClass getType();

	public boolean isResolved() {
		return TypeUtils.isResolved( boundType );
	}

	protected Type approximate() {
		return approximatedType;
	}

	protected XClass toXClass(Type type) {
		return factory.toXClass( type, context );
	}
}
