/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class DynamicResultBuilderEntityStandard
		extends AbstractFetchBuilderContainer<DynamicResultBuilderEntityStandard>
		implements DynamicResultBuilderEntity, NativeQuery.RootReturn {
	private final NavigablePath navigablePath;

	private final EntityMappingType entityMapping;
	private final String tableAlias;

	private LockMode lockMode;

	private String discriminatorColumnName;

	public DynamicResultBuilderEntityStandard(EntityMappingType entityMapping, String tableAlias) {
		this( entityMapping, tableAlias, null );
	}

	public DynamicResultBuilderEntityStandard(
			EntityMappingType entityMapping,
			String tableAlias,
			String discriminatorColumnName) {
		this.navigablePath = new NavigablePath( entityMapping.getEntityName() );

		this.entityMapping = entityMapping;
		this.tableAlias = tableAlias;

		this.discriminatorColumnName = discriminatorColumnName;
	}

	public EntityMappingType getEntityMapping() {
		return entityMapping;
	}

	@Override
	protected String getPropertyBase() {
		return entityMapping.getEntityName();
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			Consumer<SqlSelection> sqlSelectionConsumer,
			DomainResultCreationState domainResultCreationState) {
//		final FromClauseAccessImpl fromClauseAccess = ResultsHelper.extractFromClauseAccess( domainResultCreationState );
//		final TableGroup tableGroup = fromClauseAccess.resolveTableGroup(
//				navigablePath,
//				np -> {
//					final TableGroupImpl.TableReferenceImpl tableReference = new TableGroupImpl.TableReferenceImpl(
//							entityMapping.getEntityName(),
//							tableAlias,
//							false,
//							domainResultCreationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
//					);
//					return new TableGroupImpl( navigablePath, tableAlias, tableReference, entityMapping, lockMode );
//				}
//		);
//
//		return new EntityResultImpl(
//				entityMapping,
//				tableAlias,
//				lockMode,
//				jdbcResultsMetadata,
//				sqlSelectionConsumer,
//				() -> {
//					if ( discriminatorColumnName == null ) {
//						return null;
//					}
//
//					final int jdbcPosition;
//					try {
//						jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( discriminatorColumnName );
//					}
//					catch (Exception e) {
//						return null;
//					}
//
//					final int valuesArrayPosition = jdbcPosition - 1;
//
//					final SqlSelection discriminatorSqlSelection = new SqlSelectionImpl(
//							valuesArrayPosition,
//							entityMapping.getDiscriminatorMapping()
//					);
//
//					sqlSelectionConsumer.accept( discriminatorSqlSelection );
//
//					return discriminatorSqlSelection;
//				},
//				// fetchableName -> fetchBuilders.get( fetchableName ),
//				fetchableName -> null,
//				legacyFetchResolver,
//				domainResultCreationState
//		);

		throw new NotYetImplementedFor6Exception( getClass() );
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
}
