/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;

/**
 * @author Steve Ebersole
 *
 * @deprecated Use {@link org.hibernate.boot.UnsupportedOrmXsdVersionException} instead
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
