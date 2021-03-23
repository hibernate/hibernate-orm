/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptorIndicatorCapable<J> extends BasicType<J> {
	/**
	 * For Types whose resolution can be affected by SqlTypeDescriptorIndicators
	 */
	<X> BasicType<X> resolveIndicatedType(JdbcTypeDescriptorIndicators indicators);
}
