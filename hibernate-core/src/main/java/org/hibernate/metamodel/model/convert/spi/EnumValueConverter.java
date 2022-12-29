/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Remove;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuery;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.metamodel.model.convert.internal.EnumHelper.getEnumeratedValues;

/**
 * {@link BasicValueConverter} extension for enum-specific support
 *
 * @author Steve Ebersole
 */
public interface EnumValueConverter<O extends Enum<O>, R> extends BasicValueConverter<O,R> {
	@Override
	EnumJavaType<O> getDomainJavaType();

	int getJdbcTypeCode();

	String toSqlLiteral(Object value);

}
