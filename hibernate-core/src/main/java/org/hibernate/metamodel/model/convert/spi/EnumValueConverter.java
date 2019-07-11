/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;

/**
 * BasicValueConverter extension for enum-specific support
 *
 * @author Steve Ebersole
 */
public interface EnumValueConverter<O extends Enum, R> extends BasicValueConverter<O,R> {
	@Override
	EnumJavaTypeDescriptor<O> getDomainJavaDescriptor();

	int getJdbcTypeCode();

	String toSqlLiteral(Object value);
}
