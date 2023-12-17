/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.metamodel.model.domain.internal.BasicTypeImpl;
import org.hibernate.query.BindableType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcParameter
		implements JdbcParameter, JdbcParameterBinder, MappingModelExpressible, SqlExpressible, BasicValuedMapping {

	private final JdbcMapping jdbcMapping;

	public AbstractJdbcParameter(JdbcMapping jdbcMapping) {
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public JdbcParameterBinder getParameterBinder() {
		return this;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public MappingType getMappedType() {
		return jdbcMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitParameter( this );
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType javaType,
			TypeConfiguration typeConfiguration) {
		// todo (6.0) : investigate "virtual" or "static" selections
		//		- anything that is the same for each row always - parameter, literal, etc;
		//			the idea would be to write the value directly into the JdbcValues array
		//			and not generating a SQL selection in the query sent to DB
		return new SqlSelectionImpl( jdbcPosition, valuesArrayPosition, javaType, this, false );
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return jdbcMapping;
	}


	@Override
	public void bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			JdbcParameterBindings jdbcParamBindings,
			ExecutionContext executionContext) throws SQLException {
		final JdbcParameterBinding binding = jdbcParamBindings.getBinding( AbstractJdbcParameter.this );
		if ( binding == null ) {
			throw new ExecutionException( "JDBC parameter value not bound - " + this );
		}

		final JdbcMapping jdbcMapping = jdbcMapping( executionContext, binding );
		bindParameterValue( jdbcMapping, statement, binding.getBindValue(), startPosition, executionContext );
	}

	private JdbcMapping jdbcMapping(ExecutionContext executionContext, JdbcParameterBinding binding) {
		JdbcMapping jdbcMapping = binding.getBindType();

		if ( jdbcMapping == null ) {
			jdbcMapping = this.jdbcMapping;
		}

		// If the parameter type is not known from the context i.e. null or Object, infer it from the bind value
		if ( jdbcMapping == null || jdbcMapping.getMappedJavaType().getJavaTypeClass() == Object.class ) {
			jdbcMapping = guessBindType( executionContext, binding.getBindValue(), jdbcMapping );
		}

		if ( jdbcMapping == null ) {
			throw new ExecutionException( "No JDBC mapping could be inferred for parameter - " + this );
		}

		return jdbcMapping;
	}

	protected void bindParameterValue(
			JdbcMapping jdbcMapping,
			PreparedStatement statement,
			Object bindValue,
			int startPosition,
			ExecutionContext executionContext) throws SQLException {
		//noinspection unchecked
		jdbcMapping.getJdbcValueBinder().bind(
				statement,
				bindValue,
				startPosition,
				executionContext.getSession()
		);
	}

	private JdbcMapping guessBindType(ExecutionContext executionContext, Object bindValue, JdbcMapping jdbcMapping) {
		if ( bindValue == null && jdbcMapping != null ) {
			return jdbcMapping;
		}
		else {
			final BindableType<?> parameterType =
					executionContext.getSession().getFactory().getMappingMetamodel()
							.resolveParameterBindType( bindValue );
			if ( parameterType == null && bindValue instanceof Enum ) {
				return createEnumType( executionContext, (Class) bindValue.getClass() );
			}
			else {
				return parameterType instanceof JdbcMapping ? (JdbcMapping) parameterType : null;
			}
		}
	}

	private static <E extends Enum<E>> BasicTypeImpl<E> createEnumType(ExecutionContext executionContext, Class<E> enumClass) {
		final EnumJavaType<E> enumJavaType = new EnumJavaType<>( enumClass );
		final JdbcTypeIndicators indicators =
				executionContext.getSession().getTypeConfiguration().getCurrentBaseSqlTypeIndicators();
		final JdbcType jdbcType =
				// we don't know whether to map the enum as ORDINAL or STRING,
				// so just accept the default from the TypeConfiguration, which
				// is usually ORDINAL (the default according to JPA)
				enumJavaType.getRecommendedJdbcType(indicators);
		return new BasicTypeImpl<>( enumJavaType, jdbcType );
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return this;
	}

	@Override
	public int getJdbcTypeCount() {
		return 1;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return;
		}
		final JdbcMapping jdbcMapping = getJdbcMapping();
		final BasicValueConverter converter = jdbcMapping.getValueConverter();

		final Serializable disassemble;
		final int hashCode;
		if ( converter == null ) {
			final JavaType javaTypeDescriptor = jdbcMapping.getJavaTypeDescriptor();
			disassemble = javaTypeDescriptor.getMutabilityPlan().disassemble( value, session );
			hashCode = javaTypeDescriptor.extractHashCode( value );
		}
		else {
			final Object relationalValue = converter.toRelationalValue( value );
			final JavaType relationalJavaType = converter.getRelationalJavaType();
			disassemble = relationalJavaType.getMutabilityPlan().disassemble( relationalValue, session );
			hashCode = relationalJavaType.extractHashCode( relationalValue );
		}
		cacheKey.addValue( disassemble );
		cacheKey.addHashCode( hashCode );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, x, y, value, jdbcMapping );
		return getJdbcTypeCount();
	}
}
