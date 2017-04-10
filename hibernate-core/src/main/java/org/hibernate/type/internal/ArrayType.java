/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.internal.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.type.spi.Type;

/**
 * @author Andrea Boriero
 */
public class ArrayType extends AbstractCollectionType {
	private final Class elementClass;
	private final Class arrayClass;

	public ArrayType(String roleName, Class elementClass) {
		super( roleName );
		this.elementClass = elementClass;
		arrayClass = Array.newInstance( elementClass, 0 ).getClass();
	}

	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentArrayHolder( session, persister );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object array) {
		return new PersistentArrayHolder( session, array );
	}

	@Override
	public Class getReturnedClass() {
		return arrayClass;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return "null";
		}
		int length = Array.getLength( value );
		List list = new ArrayList( length );
		Type elemType = getElementType();
		for ( int i = 0; i < length; i++ ) {
			list.add( elemType.toLoggableString( Array.get( value, i ), factory ) );
		}
		return list.toString();
	}

	@Override
	public Object indexOf(Object array, Object element) {
		int length = Array.getLength( array );
		for ( int i = 0; i < length; i++ ) {
			//TODO: proxies!
			if ( Array.get( array, i ) == element ) {
				return i;
			}
		}
		return null;
	}

	@Override
	protected Iterator getElementsIterator(Object collection) {
		return Arrays.asList( (Object[]) collection ).iterator();

	}

	@Override
	protected Object replaceElements(
			Object original, Object target, Object owner, Map copyCache, SharedSessionContractImplementor session) {
		int length = Array.getLength( original );
		if ( length != Array.getLength( target ) ) {
			//note: this affects the return value!
			target = instantiateResult( original );
		}

		Type elemType = getElementType();
		for ( int i = 0; i < length; i++ ) {
			Array.set( target, i, elemType.replace( Array.get( original, i ), null, session, owner, copyCache ) );
		}

		return target;
	}

	@Override
	protected Object instantiateResult(Object original) {
		return Array.newInstance( elementClass, Array.getLength( original ) );
	}
}
