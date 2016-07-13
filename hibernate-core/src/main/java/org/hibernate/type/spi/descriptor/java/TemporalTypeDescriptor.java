/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;

/**
 * @author Steve Ebersole
 */
public interface TemporalTypeDescriptor<T> extends JavaTypeDescriptor<T> {
	javax.persistence.TemporalType getPrecision();

	<X> TemporalTypeDescriptor<X> resolveTypeForPrecision(
			javax.persistence.TemporalType precision,
			TypeDescriptorRegistryAccess scope);
}
