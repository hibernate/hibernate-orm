// $Id:$
package org.hibernate.annotations.common.reflection.java;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeSwitch;
import org.hibernate.annotations.common.reflection.java.generics.TypeUtils;

/**
 * @author Emmanuel Bernard
 * @author Paolo Perrotta
 */
@SuppressWarnings("unchecked")
class JavaXCollectionType extends JavaXType {

	public JavaXCollectionType(Type type, TypeEnvironment context, JavaReflectionManager factory) {
		super( type, context, factory );
	}

	public boolean isArray() {
		return false;
	}

	public boolean isCollection() {
		return true;
	}

	public XClass getElementClass() {
		return new TypeSwitch<XClass>() {
			@Override
			public XClass caseParameterizedType(ParameterizedType parameterizedType) {
				Type[] args = parameterizedType.getActualTypeArguments();
				Type componentType;
				Class<? extends Collection> collectionClass = getCollectionClass();
				if ( Map.class.isAssignableFrom( collectionClass )
						|| SortedMap.class.isAssignableFrom( collectionClass ) ) {
					componentType = args[1];
				}
				else {
					componentType = args[0];
				}
				return toXClass( componentType );
			}
		}.doSwitch( approximate() );
	}

	public XClass getMapKey() {
		return new TypeSwitch<XClass>() {
			@Override
			public XClass caseParameterizedType(ParameterizedType parameterizedType) {
				if ( Map.class.isAssignableFrom( getCollectionClass() ) ) {
					return toXClass( parameterizedType.getActualTypeArguments()[0] );
				}
				return null;
			}
		}.doSwitch( approximate() );
	}

	public XClass getClassOrElementClass() {
		return toXClass( approximate() );
	}

	public Class<? extends Collection> getCollectionClass() {
		return TypeUtils.getCollectionClass( approximate() );
	}

	public XClass getType() {
		return toXClass( approximate() );
	}
}