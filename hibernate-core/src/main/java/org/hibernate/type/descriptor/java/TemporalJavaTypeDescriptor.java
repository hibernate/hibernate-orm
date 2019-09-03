/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.Incubating;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Specialized JavaTypeDescriptor for temporal types.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface TemporalJavaTypeDescriptor<T> extends JavaTypeDescriptor<T> {
	/**
	 * The precision represented by this type
	 */
	javax.persistence.TemporalType getPrecision();

	/**
	 * Resolve the appropriate TemporalJavaTypeDescriptor for the given precision
	 * "relative" to this type.
	 */
	<X> TemporalJavaTypeDescriptor<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision,
			TypeConfiguration typeConfiguration);
}
