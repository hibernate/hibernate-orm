/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.access;

import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Chris Cranford
 */
public class SimpleAttributeAccessorImpl extends PropertyAccessStrategyBasicImpl {
	public static boolean invoked;
	@Override
	public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName) {
		invoked = true;
		return super.buildPropertyAccess( containerJavaType, propertyName );
	}
}
