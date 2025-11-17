/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import microsoft.sql.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLServerVectorJdbcType extends SQLServerCastingVectorJdbcType {

	public SQLServerVectorJdbcType(JdbcType elementJdbcType, int sqlType) {
		super( elementJdbcType, sqlType );
	}

	@Override
	public Expression wrapTopLevelSelectionExpression(Expression expression) {
		return expression;
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( writeExpression );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getValue( rs.getObject( paramIndex, Vector.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getValue( statement.getObject( index, Vector.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getValue( statement.getObject( name, Vector.class ), options );
			}

			private X getValue(Vector vector, WrapperOptions options) {
				if ( vector == null ) {
					return null;
				}
				return getJavaType().wrap( vector.getData(), options );
			}

		};
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setObject( index, getBindValue( value, options ), microsoft.sql.Types.VECTOR );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, getBindValue( value, options ), microsoft.sql.Types.VECTOR );
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, microsoft.sql.Types.VECTOR );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, microsoft.sql.Types.VECTOR );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) {
				final Float[] floats = getJavaType().unwrap( value, Float[].class, options );
				return new Vector( floats.length, Vector.VectorDimensionType.FLOAT32, floats );
			}
		};
	}

}
