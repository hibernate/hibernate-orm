/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.DiscriminatorType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;

/**
 * @implNote `discriminatorType` represents the mapping to Class, whereas `discriminatorType.getUnderlyingType()`
 * represents the "raw" JDBC mapping (String, Integer, etc)
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDiscriminatorMapping implements EntityDiscriminatorMapping {
	private final NavigableRole role;

	private final JdbcMapping jdbcMapping;

	private final EntityMappingType entityDescriptor;
	private final Map<Object, DiscriminatorValueDetails> valueMappings;

	private final DiscriminatorType<Object> discriminatorType;

	public AbstractDiscriminatorMapping(
			EntityMappingType entityDescriptor,
			DiscriminatorType<?> discriminatorType,
			Map<Object, DiscriminatorValueDetails> valueMappings,
			MappingModelCreationProcess creationProcess) {
		this.jdbcMapping = discriminatorType.getUnderlyingType().getJdbcMapping();
		this.entityDescriptor = entityDescriptor;
		this.valueMappings = valueMappings;

		this.role = entityDescriptor.getNavigableRole().append( EntityDiscriminatorMapping.ROLE_NAME );

		//noinspection unchecked
		this.discriminatorType = (DiscriminatorType<Object>) discriminatorType;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public BasicType<?> getUnderlyingJdbcMappingType() {
		return discriminatorType.getUnderlyingType();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityDiscriminatorMapping

	@Override
	public NavigableRole getNavigableRole() {
		return role;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return discriminatorType;
	}

	@Override
	public DiscriminatorValueDetails resolveDiscriminatorValue(Object value) {
		if ( value == null ) {
			return valueMappings.get( NULL_DISCRIMINATOR );
		}

		final DiscriminatorValueDetails matchedType = valueMappings.get( value );
		if ( matchedType != null ) {
			return matchedType;
		}

		return valueMappings.get( NOT_NULL_DISCRIMINATOR );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityDescriptor;
	}

	@Override
	public MappingType getMappedType() {
		return getJdbcMapping();
	}

	@Override
	public JavaType<?> getJavaType() {
		return getJdbcMapping().getJavaTypeDescriptor();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		// create a SqlSelection based on the underlying JdbcMapping
		final SqlSelection sqlSelection = resolveSqlSelection(
				navigablePath,
				jdbcMapping,
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);

		// return a BasicResult with conversion the entity class or entity-name
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				discriminatorType.getJavaTypeDescriptor(),
				discriminatorType.getValueConverter(),
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			FetchParent fetchParent,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
		return expressionResolver.resolveSqlSelection(
				resolveSqlExpression( navigablePath, jdbcMappingToUse, tableGroup, creationState ),
				jdbcMappingToUse.getJdbcJavaType(),
				fetchParent,
				creationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public BasicFetch<?> generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		assert tableGroup != null;

		// create a SqlSelection based on the underlying JdbcMapping
		final SqlSelection sqlSelection = resolveSqlSelection(
				fetchablePath,
				jdbcMapping,
				tableGroup,
				fetchParent,
				creationState.getSqlAstCreationState()
		);

		// return a BasicFetch with conversion the entity class or entity-name
		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				discriminatorType.getValueConverter(),
				fetchTiming,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection(
				navigablePath,
				jdbcMapping,
				tableGroup,
				null,
				creationState.getSqlAstCreationState()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept(
				resolveSqlSelection( navigablePath, jdbcMapping, tableGroup, null, creationState.getSqlAstCreationState() ),
				getJdbcMapping()
		);
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, jdbcMapping );
		return getJdbcTypeCount();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( disassemble( domainValue, session ), this );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

}
