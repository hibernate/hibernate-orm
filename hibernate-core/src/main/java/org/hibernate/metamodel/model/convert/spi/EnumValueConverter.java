/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;

/**
 * BasicValueConverter extension for enum-specific support
 *
 * @author Steve Ebersole
 */
public interface EnumValueConverter<O extends Enum, R> extends BasicValueConverter<O,R> {
	EnumJavaTypeDescriptor<O> getJavaDescriptor();
	int getJdbcTypeCode();

	O readValue(ResultSet resultSet, String name) throws SQLException;
	void writeValue(PreparedStatement statement, O value, int position) throws SQLException;

	String toSqlLiteral(Object value);
}
