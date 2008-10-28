package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.Type;
import java.util.Collection;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;

/**
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
class JavaXSimpleType extends JavaXType {

	public JavaXSimpleType(Type type, TypeEnvironment context, JavaReflectionManager factory) {
		super( type, context, factory );
	}

	public boolean isArray() {
		return false;
	}

	public boolean isCollection() {
		return false;
	}

	public XClass getElementClass() {
		return toXClass( approximate() );
	}

	public XClass getClassOrElementClass() {
		return getElementClass();
	}

	public Class<? extends Collection> getCollectionClass() {
		return null;
	}

	public XClass getType() {
		return toXClass( approximate() );
	}

	public XClass getMapKey() {
		return null;
	}
}