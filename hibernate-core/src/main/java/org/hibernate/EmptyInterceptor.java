/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.type.Type;

/**
 * An interceptor that does nothing.  May be used as a base class for application-defined custom interceptors.
 * 
 * @author Gavin King
 *
 * @deprecated implement {@link Interceptor} directly
 */
@Deprecated
public class EmptyInterceptor implements Interceptor, Serializable {
	/**
	 * The singleton reference.
	 */
	public static final Interceptor INSTANCE = new EmptyInterceptor();

	protected EmptyInterceptor() {}

	@Override
	public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		return false;
	}

	@Override
	public Object instantiate(String entityName, RepresentationMode representationMode, Object id) throws CallbackException {
		return null;
	}
}
