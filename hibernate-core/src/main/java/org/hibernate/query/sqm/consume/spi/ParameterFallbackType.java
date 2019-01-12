/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
class ParameterFallbackType implements BasicValuedExpressableType<Object> {
	private final BasicJavaDescriptor<Object> jtd;

	private final SqlExpressableType jdbcType;

	ParameterFallbackType(SessionFactoryImplementor sessionFactory) {
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		this.jtd = (BasicJavaDescriptor<Object>) typeConfiguration.getJavaTypeDescriptorRegistry().getOrMakeJavaDescriptor( Object.class );

		this.jdbcType = new SqlExpressableTypeImpl();
	}

	@Override
	public BasicJavaDescriptor<Object> getJavaTypeDescriptor() {
		return jtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return jdbcType;
	}

	@Override
	public Class<Object> getJavaType() {
		return Object.class;
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	private class SqlExpressableTypeImpl implements SqlExpressableType {
		private final SqlTypeDescriptorImpl std = new SqlTypeDescriptorImpl();

		private final JdbcValueBinder binder = new JdbcValueBinder() {
			@Override
			public void bind(
					PreparedStatement statement,
					int parameterPosition,
					Object value,
					ExecutionContext executionContext) {

			}

			@Override
			public void bind(
					CallableStatement statement,
					String parameterName,
					Object value,
					ExecutionContext executionContext) {

			}
		};

		private final JdbcValueExtractor extractor = new JdbcValueExtractor() {
			@Override
			public Object extract(
					ResultSet resultSet,
					int jdbcParameterPosition,
					ExecutionContext executionContext) {
				throw new UnsupportedOperationException( "Cannot be used for extraction" );
			}

			@Override
			public Object extract(
					CallableStatement statement,
					int jdbcParameterPosition,
					ExecutionContext executionContext) {
				throw new UnsupportedOperationException( "Cannot be used for extraction" );
			}

			@Override
			public Object extract(
					CallableStatement statement,
					String jdbcParameterName,
					ExecutionContext executionContext) {
				throw new UnsupportedOperationException( "Cannot be used for extraction" );
			}
		};

		private class SqlTypeDescriptorImpl implements SqlTypeDescriptor {

			@Override
			public boolean canBeRemapped() {
				return false;
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
				return getJavaTypeDescriptor();
			}

			@Override
			public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
				return null;
			}

			@Override
			public <T> SqlExpressableType getSqlExpressableType(BasicJavaDescriptor<T> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
				return SqlExpressableTypeImpl.this;
			}

			@Override
			public int getJdbcTypeCode() {
				return Types.JAVA_OBJECT;
			}
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return ParameterFallbackType.this.jtd;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return std;
		}

		@Override
		public JdbcValueExtractor getJdbcValueExtractor() {
			return extractor;
		}

		@Override
		public JdbcValueBinder getJdbcValueBinder() {
			return binder;
		}
	}

	@Override
	public void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( jdbcType );
	}
}
