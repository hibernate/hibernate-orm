/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

/**
 * Extension for implementations of {@link BasicType} which have an implied
 * {@linkplain BasicValueConverter conversion}.
 */
public interface ConvertedBasicType<J> extends BasicType<J> {
	BasicValueConverter<J,?> getValueConverter();
}
