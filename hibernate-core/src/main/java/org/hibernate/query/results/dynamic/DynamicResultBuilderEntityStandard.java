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
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.complete.CompleteFetchBuilder;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicResultBuilderEntityStandard
		extends AbstractFetchBuilderContainer<DynamicResultBuilderEntityStandard>
		implements DynamicResultBuilderEntity, NativeQuery.RootReturn {

	private static final String ELEMENT_PREFIX = CollectionPart.Nature.ELEMENT.getName() + ".";
	private static final String INDEX_PREFIX = CollectionPart.Nature.INDEX.getName() + ".";

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
		return entityMapping.getJavaType().getJavaTypeClass();
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
	public LockMode getLockMode() {
		return lockMode;
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
				null,
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
						parent.resolveNavigablePath( fetchable ),
						FetchTiming.IMMEDIATE,
						true,
						null,
						domainResultCreationState
				),
				fetchable,
				jdbcResultsMetadata,
				domainResultCreationState
		);
	}

	private <T> T buildResultOrFetch(
			Function<TableGroup, T> resultOrFetchBuilder,
			Fetchable fetchable,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationState = impl( domainResultCreationState );
		final FromClauseAccess fromClauseAccess = domainResultCreationState.getSqlAstCreationState().getFromClauseAccess();
		final TableGroup collectionTableGroup;
		final NavigablePath elementNavigablePath;
		if ( fetchable instanceof PluralAttributeMapping ) {
			collectionTableGroup = fromClauseAccess.getTableGroup( navigablePath );
			elementNavigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() );
		}
		else {
			collectionTableGroup = null;
			elementNavigablePath = navigablePath;
		}
		final TableGroup tableGroup = fromClauseAccess.resolveTableGroup(
				elementNavigablePath,
				np -> {
					if ( lockMode != null ) {
						domainResultCreationState.getSqlAstCreationState().registerLockMode( tableAlias, lockMode );
					}
					return entityMapping.createRootTableGroup(
							true,
							navigablePath,
							tableAlias,
							new SqlAliasBaseConstant( tableAlias ),
							null,
							creationState
					);
				}
		);
		final TableReference tableReference = tableGroup.getPrimaryTableReference();
		final List<String> keyColumnAliases;
		final FetchBuilder idFetchBuilder = findIdFetchBuilder();
		if ( this.idColumnNames != null ) {
			keyColumnAliases = this.idColumnNames;
		}
		else if ( idFetchBuilder != null ) {
			keyColumnAliases = ( (DynamicFetchBuilder) idFetchBuilder ).getColumnAliases();
		}
		else {
			keyColumnAliases = null;
		}
		if ( keyColumnAliases != null ) {
			final ModelPart keyPart;
			final TableGroup keyTableGroup;
			if ( fetchable instanceof PluralAttributeMapping ) {
				final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
				if ( pluralAttributeMapping.getCollectionDescriptor().isOneToMany() ) {
					keyPart = entityMapping.getIdentifierMapping();
					keyTableGroup = tableGroup;
				}
				else {
					// In here, we know that the idColumnNames refer to the columns of the join table,
					// so we also need to resolve selections for the element identifier columns
					final ManyToManyCollectionPart elementDescriptor = (ManyToManyCollectionPart) pluralAttributeMapping.getElementDescriptor();
					keyPart = elementDescriptor.getForeignKeyDescriptor().getKeyPart();
					keyTableGroup = collectionTableGroup;
					final List<String> idColumnAliases;
					if ( idFetchBuilder instanceof DynamicFetchBuilder ) {
						idColumnAliases = ( (DynamicFetchBuilder) idFetchBuilder ).getColumnAliases();
					}
					else {
						idColumnAliases = ( (CompleteFetchBuilder) idFetchBuilder ).getColumnAliases();
					}

					entityMapping
							.getIdentifierMapping()
							.forEachSelectable( (selectionIndex, selectableMapping) -> resolveSqlSelection(
									idColumnAliases.get( selectionIndex ),
									tableReference,
									selectableMapping,
									jdbcResultsMetadata,
									domainResultCreationState
							)
					);
				}
			}
			else {
				keyPart = entityMapping.getIdentifierMapping();
				keyTableGroup = tableGroup;
			}

			keyPart.forEachSelectable( (selectionIndex, selectableMapping) -> resolveSqlSelection(
					keyColumnAliases.get( selectionIndex ),
					keyTableGroup.resolveTableReference( selectableMapping.getContainingTableExpression() ),
					selectableMapping,
					jdbcResultsMetadata,
					domainResultCreationState
			) );
		}

		if ( discriminatorColumnName != null ) {
			resolveSqlSelection(
					discriminatorColumnName,
					tableReference,
					entityMapping.getDiscriminatorMapping(),
					jdbcResultsMetadata,
					domainResultCreationState
			);
		}

		try {
			final Map.Entry<String, NavigablePath> currentRelativePath = creationState.getCurrentRelativePath();
			final String prefix;
			if ( currentRelativePath == null ) {
				prefix = "";
			}
			else {
				prefix = currentRelativePath.getKey()
						.replace( ELEMENT_PREFIX, "" )
						.replace( INDEX_PREFIX, "" ) + ".";
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

	private FetchBuilder findIdFetchBuilder() {
		final EntityIdentifierMapping identifierMapping = entityMapping.getIdentifierMapping();
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			return findFetchBuilder( ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName() );
		}
		return findFetchBuilder( identifierMapping.getPartName() );
	}

	private void resolveSqlSelection(
			String columnAlias,
			TableReference tableReference,
			SelectableMapping selectableMapping,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		creationStateImpl.resolveSqlSelection(
				ResultsHelper.resolveSqlExpression(
						creationStateImpl,
						jdbcResultsMetadata,
						tableReference,
						selectableMapping,
						columnAlias
				),
				selectableMapping.getJdbcMapping().getJdbcJavaType(),
				null,
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
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
