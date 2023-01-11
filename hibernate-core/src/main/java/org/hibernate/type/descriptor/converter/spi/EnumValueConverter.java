/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.spi;

import org.hibernate.Incubating;
import org.hibernate.type.descriptor.java.EnumJavaType;

/**
 * {@link BasicValueConverter} extension for enum-specific support
 *
 * @author Steve Ebersole
 */
@Incubating
public interface EnumValueConverter<O extends Enum<O>, R> extends BasicValueConverter<O,R> {
	@Override
	EnumJavaType<O> getDomainJavaType();

	int getJdbcTypeCode();

	String toSqlLiteral(Object value);

}
