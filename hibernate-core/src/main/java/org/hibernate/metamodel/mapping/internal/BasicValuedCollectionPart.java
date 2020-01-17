/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models a basic collection element/value or index/key
 *
 * @author Steve Ebersole
 */
public class BasicValuedCollectionPart implements CollectionPart, BasicValuedModelPart {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;
	private final BasicType mapper;
	private final BasicValueConverter valueConverter;

	private final String tableExpression;
	private final String columnExpression;

	public BasicValuedCollectionPart(
			CollectionPersister collectionDescriptor,
			Nature nature,
			BasicType mapper,
			BasicValueConverter valueConverter,
			String tableExpression,
			String columnExpression) {
		this.navigableRole = collectionDescriptor.getNavigableRole().append( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.nature = nature;
		this.mapper = mapper;
		this.valueConverter = valueConverter;
		this.tableExpression = tableExpression;
		this.columnExpression = columnExpression;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public MappingType getPartMappingType() {
		return mapper;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public String getMappedColumnExpression() {
		return columnExpression;
	}

	@Override
	public BasicValueConverter getConverter() {
		return valueConverter;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return mapper.getMappedJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				getJavaTypeDescriptor(),
				valueConverter,
				navigablePath
		);
	}

	private SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState) {
		final SqlExpressionResolver exprResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

		return exprResolver.resolveSqlSelection(
				exprResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( tableGroup.getPrimaryTableReference(), columnExpression ),
						sqlAstProcessingState -> new ColumnReference(
								tableGroup.getPrimaryTableReference().getIdentificationVariable(),
								columnExpression,
								mapper,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				getJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {

	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return mapper;
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return mapper;
	}

	@Override
	public String getFetchableName() {
		return nature == Nature.ELEMENT ? "{value}" : "{key}";
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return FetchStrategy.IMMEDIATE_JOIN;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		ResultsLogger.INSTANCE.debugf(
				"Generating Fetch for collection-part : `%s` -> `%s`",
				collectionDescriptor.getRole(),
				nature.getName()
		);

		final TableGroup tableGroup = creationState.getSqlAstCreationState()
				.getFromClauseAccess()
				.findTableGroup( fetchablePath.getParent() );
		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		return new BasicFetch(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				false,
				valueConverter,
				FetchTiming.IMMEDIATE,
				creationState
		);
	}

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return 1;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		return Collections.singletonList( getJdbcMapping() );
	}

	@Override
	public BasicType getBasicType() {
		return mapper;
	}
}
