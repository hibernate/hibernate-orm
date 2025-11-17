/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.hibernate.vector.internal.VectorHelper.parseFloatVector;

public class SQLServerCastingVectorJdbcType extends ArrayJdbcType {

	private final int sqlType;

	public SQLServerCastingVectorJdbcType(JdbcType elementJdbcType, int sqlType) {
		super( elementJdbcType );
		this.sqlType = sqlType;
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
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( float[].class );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new SQLServerJdbcLiteralFormatterVector<>(
				javaTypeDescriptor,
				getElementJdbcType().getJdbcLiteralFormatter( elementJavaType( javaTypeDescriptor ) )
		);
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping, @Nullable Size size) {
		return sourceMapping.getJdbcType().isStringLike() ? "cast(?1 as vector(" + size.getArrayLength() + "))" : null;
	}

	@Override
	public Expression wrapTopLevelSelectionExpression(Expression expression) {
		return new SelfRenderingExpression() {
			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstTranslator<?> walker,
					SessionFactoryImplementor sessionFactory) {
				sqlAppender.append( "cast(" );
				expression.accept( walker );
				sqlAppender.append( " as nvarchar(max))" );
			}

			@Override
			public JdbcMappingContainer getExpressionType() {
				return expression.getExpressionType();
			}
		};
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.appendSql( "cast(" );
		appender.appendSql( writeExpression );
		appender.appendSql( " as vector(" );
		appender.appendSql( size.getArrayLength() );
		appender.appendSql( "))" );
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
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setString( index, getBindValue( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, getBindValue( value, options ) );
			}

			@Override
			public String getBindValue(X value, WrapperOptions options) {
				return Arrays.toString( getJavaType().unwrap( value, float[].class, options ) );
			}
		};
	}

	@Override
	public boolean equals(Object that) {
		return super.equals( that )
			&& that instanceof SQLServerCastingVectorJdbcType vectorJdbcType
			&& sqlType == vectorJdbcType.sqlType;
	}

	@Override
	public int hashCode() {
		return sqlType + 31 * super.hashCode();
	}
}
