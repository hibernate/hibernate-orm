/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hibernate.vector.internal.VectorHelper.parseFloatVector;

public class PGVectorJdbcType extends ArrayJdbcType {

	private final int sqlType;
	private final String typeName;

	public PGVectorJdbcType(JdbcType elementJdbcType, int sqlType, String typeName) {
		super( elementJdbcType );
		this.sqlType = sqlType;
		this.typeName = typeName;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return sqlType;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( float[].class );
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "cast(" );
		appender.append( writeExpression );
		appender.append( " as " );
		appender.append( typeName );
		appender.append( ')' );
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping) {
		return sourceMapping.getJdbcType().isStringLike() ? "cast(?1 as " + typeName + ")" : null;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseFloatVector( rs.getString( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseFloatVector( statement.getString( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseFloatVector( statement.getString( name ) ), options );
			}
		};
	}

	@Override
	public boolean equals(Object that) {
		return super.equals( that )
			&& that instanceof PGVectorJdbcType vectorJdbcType
			&& sqlType == vectorJdbcType.sqlType;
	}

	@Override
	public int hashCode() {
		return sqlType + 31 * super.hashCode();
	}
}
