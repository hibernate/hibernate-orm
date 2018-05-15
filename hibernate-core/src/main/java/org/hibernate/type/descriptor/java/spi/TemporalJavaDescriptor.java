/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.Incubating;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface TemporalJavaDescriptor<T> extends BasicJavaDescriptor<T> {
	javax.persistence.TemporalType getPrecision();

	<X> TemporalJavaDescriptor<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision,
			TypeConfiguration scope);
}
