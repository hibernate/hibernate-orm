/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.beans;

import org.hibernate.HibernateException;

/**
 * Indicates a problem dealing with {@link java.beans.BeanInfo} via the {@link BeanInfoHelper} delegate.
 *
 * @author Steve Ebersole
 */
public class BeanIntrospectionException extends HibernateException {
	public BeanIntrospectionException(String string, Throwable root) {
		super( string, root );
	}

	public BeanIntrospectionException(String s) {
		super( s );
	}
}
