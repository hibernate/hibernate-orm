/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.annotationfactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A concrete implementation of <code>Annotation</code> that pretends it is a
 * "real" source code annotation. It's also an <code>InvocationHandler</code>.
 * <p/>
 * When you create an <code>AnnotationProxy</code>, you must initialize it
 * with an <code>AnnotationDescriptor</code>.
 * The adapter checks that the provided elements are the same elements defined
 * in the annotation interface. However, it does <i>not</i> check that their
 * values are the right type. If you omit an element, the adapter will use the
 * default value for that element from the annotation interface, if it exists.
 * If no default exists, it will throw an exception.
 * <p/>
 * Warning: this class does not implement <code>hashCode()</code> and
 * <code>equals()</code> - it just uses the ones it inherits from <code>Object</code>.
 * This means that an <code>AnnotationProxy</code> does <i>not</i> follow the
 * recommendations of the <code>Annotation</code> javadoc about these two
 * methods. That's why you should never mix <code>AnnotationProxies</code>
 * with "real" annotations. For example, don't put them into the same
 * <code>Collection</code>.
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 * @see java.lang.annotation.Annotation
 */
public class AnnotationProxy implements Annotation, InvocationHandler {

	private final Class<? extends Annotation> annotationType;
	//FIXME it's probably better to use String as a key rather than Method
	//      to speed up and avoid any fancy permsize/GC issue
	//      I'd better check the litterature on the subject
	private final Map<Method, Object> values;

	public AnnotationProxy(AnnotationDescriptor descriptor) {
		this.annotationType = descriptor.type();
		values = getAnnotationValues( descriptor );
	}

	private Map<Method, Object> getAnnotationValues(AnnotationDescriptor descriptor) {
		Map<Method, Object> result = new HashMap<Method, Object>();
		int processedValuesFromDescriptor = 0;
		for ( Method m : annotationType.getDeclaredMethods() ) {
			if ( descriptor.containsElement( m.getName() ) ) {
				result.put( m, descriptor.valueOf( m.getName() ) );
				processedValuesFromDescriptor++;
			}
			else if ( m.getDefaultValue() != null ) {
				result.put( m, m.getDefaultValue() );
			}
			else {
				throw new IllegalArgumentException( "No value provided for " + m.getName() );
			}
		}
		if ( processedValuesFromDescriptor != descriptor.numberOfElements() ) {
			throw new RuntimeException( "Trying to instanciate " + annotationType + " with unknown elements" );
		}
		return result;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( values.containsKey( method ) ) {
			return values.get( method );
		}
		return method.invoke( this, args );
	}

	public Class<? extends Annotation> annotationType() {
		return annotationType;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append( '@' ).append( annotationType().getName() ).append( '(' );
		for ( Method m : getRegisteredMethodsInAlphabeticalOrder() ) {
			result.append( m.getName() ).append( '=' ).append( values.get( m ) ).append( ", " );
		}
		// remove last separator:
		if ( values.size() > 0 ) {
			result.delete( result.length() - 2, result.length() );
			result.append( ")" );
		}
		else {
			result.delete( result.length() - 1, result.length() );
		}

		return result.toString();
	}

	private SortedSet<Method> getRegisteredMethodsInAlphabeticalOrder() {
		SortedSet<Method> result = new TreeSet<Method>(
				new Comparator<Method>() {
					public int compare(Method o1, Method o2) {
						return o1.getName().compareTo( o2.getName() );
					}
				}
		);
		//List<Method> result = new LinkedList<Method>();
		result.addAll( values.keySet() );
		return result;
	}
}
