/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.internal.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A type for persistent arrays.
 * @author Gavin King
 */
public class ArrayType extends CollectionType {

	private final Class elementClass;
	private final Class arrayClass;

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public ArrayType(TypeFactory.TypeScope typeScope, String role, String propertyRef, Class elementClass) {
		this( role, propertyRef, elementClass );
	}

	public ArrayType(String role, String propertyRef, Class elementClass) {
		super( role, propertyRef );
		this.elementClass = elementClass;
		arrayClass = Array.newInstance(elementClass, 0).getClass();
	}

	@Override
	public Class getReturnedClass() {
		return arrayClass;
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Serializable key)
	throws HibernateException {
		return new PersistentArrayHolder(session, persister);
	}

	/**
	 * Not defined for collections of primitive type
	 */
	@Override
	public Iterator getElementsIterator(Object collection) {
		return Arrays.asList( (Object[]) collection ).iterator();
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object array) {
		return new PersistentArrayHolder(session, array);
	}

	@Override
	public boolean isArrayType() {
		return true;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		int length = Array.getLength(value);
		List list = new ArrayList(length);
		Type elemType = getElementType(factory);
		for ( int i=0; i<length; i++ ) {
			Object element = Array.get(value, i);
			if ( element == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( element ) ) {
				list.add( "<uninitialized>" );
			}
			else {
				list.add( elemType.toLoggableString( element, factory ) );
			}
		}
		return list.toString();
	}

	@Override
	public Object instantiateResult(Object original) {
		return Array.newInstance( elementClass, Array.getLength(original) );
	}

	@Override
	public Object replaceElements(
		Object original,
		Object target,
		Object owner,
		Map copyCache,
		SharedSessionContractImplementor session) throws HibernateException {

		int length = Array.getLength(original);
		if ( length!=Array.getLength(target) ) {
			//note: this affects the return value!
			target=instantiateResult(original);
		}

		Type elemType = getElementType( session.getFactory() );
		for ( int i=0; i<length; i++ ) {
			Array.set( target, i, elemType.replace( Array.get(original, i), null, session, owner, copyCache ) );
		}

		return target;

	}

	@Override
	public Object instantiate(int anticipatedSize) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object indexOf(Object array, Object element) {
		int length = Array.getLength(array);
		for ( int i=0; i<length; i++ ) {
			//TODO: proxies!
			if ( Array.get(array, i)==element ) {
				return i;
			}
		}
		return null;
	}

	@Override
	protected boolean initializeImmediately() {
		return true;
	}

	@Override
	public boolean hasHolder() {
		return true;
	}

}
