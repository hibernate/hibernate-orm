/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 *
 * @deprecated Migrating to boot package; all usages will eventually be replaced by
 * {@link org.hibernate.boot.UnsupportedOrmXsdVersionException}
 */
@Deprecated
public class UnsupportedOrmXsdVersionException extends HibernateException {
	public UnsupportedOrmXsdVersionException(String requestedVersion, Origin origin) {
		super(
				String.format(
						"Encountered unsupported orm.xml xsd version [%s] in mapping document [type=%s, name=%s]",
						requestedVersion,
						origin.getType(),
						origin.getName()
				)
		);
	}
}
