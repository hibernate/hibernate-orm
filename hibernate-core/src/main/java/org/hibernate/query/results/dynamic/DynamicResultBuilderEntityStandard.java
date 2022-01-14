/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicResultBuilderEntityStandard
		extends AbstractFetchBuilderContainer<DynamicResultBuilderEntityStandard>
		implements DynamicResultBuilderEntity, NativeQuery.RootReturn {

	private static final String ELEMENT_PREFIX = CollectionPart.Nature.ELEMENT.getName() + ".";

	private final NavigablePath navigablePath;

	private final EntityMappingType entityMapping;
	private final String tableAlias;

	private LockMode lockMode;

	private List<String> idColumnNames;
	private String discriminatorColumnName;

	public DynamicResultBuilderEntityStandard(EntityMappingType entityMapping, String tableAlias) {
		this( entityMapping, tableAlias, new NavigablePath( entityMapping.getEntityName() ) );
	}

	public DynamicResultBuilderEntityStandard(
			EntityMappingType entityMapping,
			String tableAlias,
			NavigablePath navigablePath) {
		this.navigablePath = navigablePath;
		this.entityMapping = entityMapping;
		this.tableAlias = tableAlias;
	}

	private DynamicResultBuilderEntityStandard(DynamicResultBuilderEntityStandard original) {
		super( original );
		this.navigablePath = original.navigablePath;
		this.entityMapping = original.entityMapping;
		this.tableAlias = original.tableAlias;
		this.lockMode = original.lockMode;
		this.idColumnNames = original.idColumnNames == null ? null : List.copyOf( original.idColumnNames );
		this.discriminatorColumnName = original.discriminatorColumnName;
	}

	@Override
	public Class<?> getJavaType() {
		return entityMapping.getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public EntityMappingType getEntityMapping() {
		return entityMapping;
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NativeQuery.RootReturn addIdColumnAliases(String... aliases) {
		if ( idColumnNames == null ) {
			idColumnNames = new ArrayList<>( aliases.length );
		}
		Collections.addAll( idColumnNames, aliases );
		return this;
	}

	@Override
	public String getDiscriminatorAlias() {
		return discriminatorColumnName;
	}

	@Override
	protected String getPropertyBase() {
		return entityMapping.getEntityName();
	}

	@Override
	public DynamicResultBuilderEntityStandard cacheKeyInstance() {
		return new DynamicResultBuilderEntityStandard( this );
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		return buildResultOrFetch(
				(tableGroup) -> (EntityResult) entityMapping.createDomainResult(
						navigablePath,
						tableGroup,
						tableAlias,
						domainResultCreationState
				),
				jdbcResultsMetadata,
				domainResultCreationState
		);
	}

	public Fetch buildFetch(
			FetchParent parent,
			Fetchable fetchable,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		return buildResultOrFetch(
				(tableGroup) -> parent.generateFetchableFetch(
						fetchable,
						navigablePath,
						FetchTiming.IMMEDIATE,
						true,
						null,
						domainResultCreationState
				),
				jdbcResultsMetadata,
				domainResultCreationState
		);
	}

	private <T> T buildResultOrFetch(
			Function<TableGroup, T> resultOrFetchBuilder,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationState = impl( domainResultCreationState );
		final FromClauseAccess fromClauseAccess = domainResultCreationState.getSqlAstCreationState().getFromClauseAccess();
		final TableGroup tableGroup = fromClauseAccess.resolveTableGroup(
				navigablePath,
				np -> {
					final TableReference tableReference = entityMapping.createPrimaryTableReference(
							new SqlAliasBaseConstant( tableAlias ),
							creationState.getSqlExpressionResolver(),
							creationState.getCreationContext()
					);

					if ( lockMode != null ) {
						domainResultCreationState.getSqlAstCreationState().registerLockMode( tableAlias, lockMode );
					}
					return new TableGroupImpl( navigablePath, tableAlias, tableReference, entityMapping );
				}
		);
		final TableReference tableReference = tableGroup.getPrimaryTableReference();
		final List<String> idColumnAliases;
		final DynamicFetchBuilder idFetchBuilder;
		if ( this.idColumnNames != null ) {
			idColumnAliases = this.idColumnNames;
		}
		else if ( ( idFetchBuilder = findIdFetchBuilder() ) != null ) {
			idColumnAliases = idFetchBuilder.getColumnAliases();
		}
		else {
			idColumnAliases = null;
		}
		if ( idColumnAliases != null ) {
			final EntityIdentifierMapping identifierMapping = entityMapping.getIdentifierMapping();
			identifierMapping.forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						resolveSqlSelection(
								idColumnAliases.get( selectionIndex ),
								createColumnReferenceKey( tableReference, selectableMapping.getSelectionExpression() ),
								selectableMapping.getJdbcMapping(),
								jdbcResultsMetadata,
								domainResultCreationState
						);
					}
			);
		}

		if ( discriminatorColumnName != null ) {
			resolveSqlSelection(
					discriminatorColumnName,
					createColumnReferenceKey(
							tableReference,
							entityMapping.getDiscriminatorMapping().getSelectionExpression()
					),
					entityMapping.getDiscriminatorMapping().getJdbcMapping(),
					jdbcResultsMetadata,
					domainResultCreationState
			);
		}

		try {
			final NavigablePath currentRelativePath = creationState.getCurrentRelativePath();
			final String prefix;
			if ( currentRelativePath == null ) {
				prefix = "";
			}
			else {
				prefix = currentRelativePath.getFullPath() + ".";
			}
			creationState.pushExplicitFetchMementoResolver(
					relativePath -> {
						if ( relativePath.startsWith( prefix ) ) {
							final int startIndex;
							if ( relativePath.regionMatches( prefix.length(), ELEMENT_PREFIX, 0, ELEMENT_PREFIX.length() ) ) {
								startIndex = prefix.length() + ELEMENT_PREFIX.length();
							}
							else {
								startIndex = prefix.length();
							}
							return findFetchBuilder( relativePath.substring( startIndex ) );
						}
						return null;
					}
			);
			return resultOrFetchBuilder.apply( tableGroup );
		}
		finally {
			creationState.popExplicitFetchMementoResolver();
		}
	}

	private DynamicFetchBuilder findIdFetchBuilder() {
		final EntityIdentifierMapping identifierMapping = entityMapping.getIdentifierMapping();
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			return findFetchBuilder( ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName() );
		}
		return findFetchBuilder( identifierMapping.getPartName() );
	}

	private void resolveSqlSelection(
			String columnAlias,
			String columnKey,
			JdbcMapping jdbcMapping,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						columnKey,
						state -> {
							final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
							final int valuesArrayPosition = jdbcPosition - 1;
							return new ResultSetMappingSqlSelection( valuesArrayPosition, jdbcMapping );
						}
				),
				jdbcMapping.getMappedJavaTypeDescriptor(),
				domainResultCreationState.getSqlAstCreationState().getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public DynamicResultBuilderEntityStandard setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public DynamicResultBuilderEntityStandard setDiscriminatorAlias(String columnName) {
		this.discriminatorColumnName = columnName;
		return this;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + navigablePath.hashCode();
		result = 31 * result + entityMapping.hashCode();
		result = 31 * result + tableAlias.hashCode();
		result = 31 * result + ( lockMode != null ? lockMode.hashCode() : 0 );
		result = 31 * result + ( idColumnNames != null ? idColumnNames.hashCode() : 0 );
		result = 31 * result + ( discriminatorColumnName != null ? discriminatorColumnName.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final DynamicResultBuilderEntityStandard that = (DynamicResultBuilderEntityStandard) o;
		return navigablePath.equals( that.navigablePath )
				&& entityMapping.equals( that.entityMapping )
				&& tableAlias.equals( that.tableAlias )
				&& lockMode == that.lockMode
				&& Objects.equals( idColumnNames, that.idColumnNames )
				&& Objects.equals( discriminatorColumnName, that.discriminatorColumnName )
				&& super.equals( o );
	}
}
