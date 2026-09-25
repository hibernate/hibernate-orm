package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.sql.ast.spi.result.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableTypeLiteral
		implements Expression, DomainResultProducer<Object>, BasicValuedMapping {
	private final Class<?> embeddableClass;
	private final BasicType<?> basicType;

	public EmbeddableTypeLiteral(
			EmbeddableDomainType<?> embeddableDomainType,
			BasicType<?> basicType) {
		this.embeddableClass = embeddableDomainType.getJavaType();
		this.basicType = basicType;
	}

	public Object getEmbeddableClass() {
		return embeddableClass;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicValuedMapping

	@Nonnull
	@Override
	public MappingModelExpressible<?> getExpressionType() {
		return this;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return basicType;
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return basicType;
	}

	@Override
	public int getJdbcTypeCount() {
		return basicType.getJdbcTypeCount();
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return basicType.getJdbcMapping( index );
	}

	@Nonnull
	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return basicType.getSingleJdbcMapping();
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return basicType.forEachJdbcType( offset, action );
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return basicType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		basicType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return basicType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return basicType.forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		createSqlSelection( creationState );
	}

	@Override
	public DomainResult<Object> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return new BasicResult<>(
				createSqlSelection( creationState ).getValuesArrayPosition(),
				resultVariable,
				basicType
		);
	}

	private SqlSelection createSqlSelection(DomainResultCreationState creationState) {
		return creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				basicType.getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitEmbeddableTypeLiteral( this );
	}

	@Nonnull
	@Override
	public JavaType getExpressibleJavaType() {
		return basicType.getExpressibleJavaType();
	}
}
