/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractEntityDiscriminatorMapping implements EntityDiscriminatorMapping {
	private final EntityPersister entityDescriptor;
	private final String tableExpression;
	private final String mappedColumnExpression;

	private final BasicType mappingType;

	public AbstractEntityDiscriminatorMapping(
			EntityPersister entityDescriptor,
			String tableExpression,
			String mappedColumnExpression,
			BasicType mappingType) {
		this.entityDescriptor = entityDescriptor;
		this.tableExpression = tableExpression;
		this.mappedColumnExpression = mappedColumnExpression;
		this.mappingType = mappingType;
	}

	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public BasicType getBasicType() {
		return mappingType;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public String getMappedColumnExpression() {
		return mappedColumnExpression;
	}

	@Override
	public BasicValueConverter getConverter() {
		return null;
	}

	@Override
	public String getFetchableName() {
		return ROLE_NAME;
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return FetchStrategy.IMMEDIATE_JOIN;
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
				navigablePath
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( tableGroup, creationState );
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
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		assert tableGroup != null;

		final SqlSelection sqlSelection = resolveSqlSelection( tableGroup, creationState );

		return new BasicFetch(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				false,
				getConverter(),
				fetchTiming,
				creationState
		);
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return mappingType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return mappingType.getJdbcMapping();
	}

	protected abstract SqlSelection resolveSqlSelection(TableGroup tableGroup, DomainResultCreationState creationState);
}
