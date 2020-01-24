/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Dialect for Derby/Cloudscape 10.6
 *
 * @author Simon Johnston
 * @author Scott Marlow
 *
 * @deprecated use {@code DerbyDialect(1060)}
 */
@Deprecated
public class DerbyTenSixDialect extends DerbyDialect {

	public DerbyTenSixDialect() {
		super(1060);
	}

}
