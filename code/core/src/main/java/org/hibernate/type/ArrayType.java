//$Id: ArrayType.java 10086 2006-07-05 18:17:27Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentArrayHolder;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A type for persistent arrays.
 * @author Gavin King
 */
public class ArrayType extends CollectionType {

	private final Class elementClass;
	private final Class arrayClass;

	public ArrayType(String role, String propertyRef, Class elementClass, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
		this.elementClass = elementClass;
		arrayClass = Array.newInstance(elementClass, 0).getClass();
	}

	public Class getReturnedClass() {
		return arrayClass;
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) 
	throws HibernateException {
		return new PersistentArrayHolder(session, persister);
	}

	/**
	 * Not defined for collections of primitive type
	 */
	public Iterator getElementsIterator(Object collection) {
		return Arrays.asList( (Object[]) collection ).iterator();
	}

	public PersistentCollection wrap(SessionImplementor session, Object array) {
		return new PersistentArrayHolder(session, array);
	}

	public boolean isArrayType() {
		return true;
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		int length = Array.getLength(value);
		List list = new ArrayList(length);
		Type elemType = getElementType(factory);
		for ( int i=0; i<length; i++ ) {
			list.add( elemType.toLoggableString( Array.get(value, i), factory ) );
		}
		return list.toString();
	}
	
	public Object instantiateResult(Object original) {
		return Array.newInstance( elementClass, Array.getLength(original) );
	}

	public Object replaceElements(
		Object original,
		Object target,
		Object owner, 
		Map copyCache, 
		SessionImplementor session)
	throws HibernateException {
		
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

	public Object instantiate(int anticipatedSize) {
		throw new UnsupportedOperationException();
	}

	public Object indexOf(Object array, Object element) {
		int length = Array.getLength(array);
		for ( int i=0; i<length; i++ ) {
			//TODO: proxies!
			if ( Array.get(array, i)==element ) return new Integer(i);
		}
		return null;
	}

	protected boolean initializeImmediately(EntityMode entityMode) {
		return true;
	}

	public boolean hasHolder(EntityMode entityMode) {
		return true;
	}
	

}
