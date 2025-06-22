/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntityTypeLiteral
		implements Expression, DomainResultProducer<Object>, BasicValuedMapping {
	private final EntityPersister entityTypeDescriptor;
	private final DiscriminatorType<?> discriminatorType;

	public EntityTypeLiteral(EntityPersister entityTypeDescriptor) {
		this.entityTypeDescriptor = entityTypeDescriptor;
		this.discriminatorType = entityTypeDescriptor.getDiscriminatorDomainType();
	}

	public EntityPersister getEntityTypeDescriptor() {
		return entityTypeDescriptor;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicValuedMapping

	@Override
	public MappingModelExpressible<?> getExpressionType() {
		return this;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return discriminatorType;
	}

	@Override
	public MappingType getMappedType() {
		return discriminatorType;
	}

	@Override
	public int getJdbcTypeCount() {
		return discriminatorType.getJdbcTypeCount();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return discriminatorType.getJdbcMapping( index );
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return discriminatorType.getSingleJdbcMapping();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return discriminatorType.forEachJdbcType( offset, action );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return discriminatorType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		discriminatorType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return discriminatorType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return discriminatorType.forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
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
				discriminatorType
		);
	}

	private SqlSelection createSqlSelection(DomainResultCreationState creationState) {
		return creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				discriminatorType.getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitEntityTypeLiteral( this );
	}

	@Override
	public JavaType<?> getExpressibleJavaType() {
		return discriminatorType.getExpressibleJavaType();
	}
}
