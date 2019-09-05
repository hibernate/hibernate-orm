/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.annotations.Remove;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
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

	/**
	 * @since 6.0
	 *
	 * @deprecated Added temporarily in support of dual SQL execution until fully migrated
	 * to {@link SelectStatement} and {@link JdbcOperation}
	 */
	@Remove
	@Deprecated
	void writeValue(
			PreparedStatement statement,
			Enum value,
			int position,
			SharedSessionContractImplementor session) throws SQLException;
}
