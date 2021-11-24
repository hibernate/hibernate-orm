/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy.internal;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.model.BasicAttribute;
import org.hibernate.envers.boot.model.Column;
import org.hibernate.envers.boot.model.ManyToOneAttribute;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.RevisionInfoHelper;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.spi.AuditStrategyContext;
import org.hibernate.envers.strategy.spi.MappingContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.sql.Update;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.MapType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * An audit strategy implementation that persists and fetches audit information using a validity
 * algorithm, based on the start-revision and end-revision of a row in the audit table schema.
 * <p>
 * This algorithm works as follows:
 * <ul>
 * <li>For a new row, only the start-revision column is set in the row.</li>
 * <li>Concurrently, the end-revision of the prior audit row is set to the current revision</li>
 * <li>Queries using a between start and end revision predicate rather than using subqueries.</li>
 * </ul>
 * <p>
 * This has a few important consequences which must be considered:
 * <ul>
 * <li>Persisting audit information is sightly slower due to an extra update required</li>
 * <li>Retreiving audit information is considerably faster</li>
 * </ul>
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class ValidityAuditStrategy implements AuditStrategy {
	/**
	 * getter for the revision entity field annotated with @RevisionTimestamp
	 */
	private Getter revisionTimestampGetter;

	private final SessionCacheCleaner sessionCacheCleaner;

	public ValidityAuditStrategy() {
		sessionCacheCleaner = new SessionCacheCleaner();
	}

	@Override
	public void postInitialize(AuditStrategyContext context) {
		setRevisionTimestampGetter( context.getRevisionInfoTimestampAccessor() );
	}

	@Override
	public void addAdditionalColumns(MappingContext mappingContext) {
		final ManyToOneAttribute revEndMapping = new ManyToOneAttribute(
				mappingContext.getConfiguration().getRevisionEndFieldName(),
				mappingContext.getRevisionInfoPropertyType(),
				true,
				true,
				false,
				mappingContext.getRevisionInfoExplicitTypeName()
		);

		RevisionInfoHelper.addOrModifyColumn(
				revEndMapping,
				mappingContext.getConfiguration().getRevisionEndFieldName()
		);

		mappingContext.getEntityMapping().addAttribute( revEndMapping );

		if ( mappingContext.getConfiguration().isRevisionEndTimestampEnabled() ) {
			// add a column for the timestamp of the end revision
			final String revisionInfoTimestampTypeName = StandardBasicTypes.TIMESTAMP.getName();
			final BasicAttribute revEndTimestampMapping = new BasicAttribute(
					mappingContext.getConfiguration().getRevisionEndTimestampFieldName(),
					revisionInfoTimestampTypeName,
					true,
					true,
					false
			);
			revEndTimestampMapping.addColumn(
					new Column( mappingContext.getConfiguration().getRevisionEndTimestampFieldName() )
			);
			mappingContext.getEntityMapping().addAttribute( revEndTimestampMapping );
		}
	}

	@Override
	public void perform(
			final Session session,
			final String entityName,
			final Configuration configuration,
			final Object id,
			final Object data,
			final Object revision) {
		final String auditedEntityName = configuration.getAuditEntityName( entityName );
		final String revisionInfoEntityName = configuration.getRevisionInfo().getRevisionInfoEntityName();

		// Save the audit data
		session.save( auditedEntityName, data );

		// Update the end date of the previous row.
		//
		// When application reuses identifiers of previously removed entities:
		// The UPDATE statement will no-op if an entity with a given identifier has been
		// inserted for the first time. But in case a deleted primary key value was
		// reused, this guarantees correct strategy behavior: exactly one row with
		// null end date exists for each identifier.
		final boolean reuseEntityIdentifier = configuration.isAllowIdentifierReuse();
		if ( reuseEntityIdentifier || getRevisionType( configuration, data ) != RevisionType.ADD ) {
			// Register transaction completion process to guarantee execution of UPDATE statement after INSERT.
			( (EventSource) session ).getActionQueue().registerProcess( new BeforeTransactionCompletionProcess() {
				@Override
				public void doBeforeTransactionCompletion(final SessionImplementor sessionImplementor) {
					final Queryable productionEntityQueryable = getQueryable( entityName, sessionImplementor );
					final Queryable rootProductionEntityQueryable = getQueryable(
							productionEntityQueryable.getRootEntityName(), sessionImplementor
					);
					final Queryable auditedEntityQueryable = getQueryable( auditedEntityName, sessionImplementor );
					final Queryable rootAuditedEntityQueryable = getQueryable(
							auditedEntityQueryable.getRootEntityName(), sessionImplementor
					);

					final String updateTableName;
					if ( UnionSubclassEntityPersister.class.isInstance( rootProductionEntityQueryable ) ) {
						// this is the condition causing all the problems in terms of the generated SQL UPDATE
						// the problem being that we currently try to update the in-line view made up of the union query
						//
						// this is extremely hacky means to get the root table name for the union subclass style entities.
						// hacky because it relies on internal behavior of UnionSubclassEntityPersister
						// !!!!!! NOTICE - using subclass persister, not root !!!!!!
						updateTableName = auditedEntityQueryable.getSubclassTableName( 0 );
					}
					else {
						updateTableName = rootAuditedEntityQueryable.getTableName();
					}

					final Type revisionInfoIdType = sessionImplementor.getFactory().getMetamodel().entityPersister( revisionInfoEntityName ).getIdentifierType();
					final String revEndColumnName = rootAuditedEntityQueryable.toColumns( configuration.getRevisionEndFieldName() )[0];

					final boolean isRevisionEndTimestampEnabled = configuration.isRevisionEndTimestampEnabled();

					// update audit_ent set REVEND = ? [, REVEND_TSTMP = ?] where (prod_ent_id) = ? and REV <> ? and REVEND is null
					final Update update = new Update( sessionImplementor.getFactory().getJdbcServices().getDialect() ).setTableName( updateTableName );
					// set REVEND = ?
					update.addColumn( revEndColumnName );
					// set [, REVEND_TSTMP = ?]
					if ( isRevisionEndTimestampEnabled ) {
						update.addColumn(
								rootAuditedEntityQueryable.toColumns( configuration.getRevisionEndTimestampFieldName() )[0]
						);
					}

					// where (prod_ent_id) = ?
					update.addPrimaryKeyColumns( rootProductionEntityQueryable.getIdentifierColumnNames() );
					// where REV <> ?
					update.addWhereColumn(
							rootAuditedEntityQueryable.toColumns( configuration.getRevisionNumberPath() )[0], "<> ?"
					);
					// where REVEND is null
					update.addWhereColumn( revEndColumnName, " is null" );

					// Now lets execute the sql...
					final String updateSql = update.toStatementString();

					int rowCount = sessionImplementor.doReturningWork(
							new ReturningWork<Integer>() {
								@Override
								public Integer execute(Connection connection) throws SQLException {
									PreparedStatement preparedStatement = sessionImplementor
											.getJdbcCoordinator().getStatementPreparer().prepareStatement( updateSql );

									try {
										int index = 1;

										// set REVEND = ?
										final Number revisionNumber = configuration.getEnversService()
												.getRevisionInfoNumberReader()
												.getRevisionNumber( revision );

										revisionInfoIdType.nullSafeSet(
												preparedStatement, revisionNumber, index, sessionImplementor
										);
										index += revisionInfoIdType.getColumnSpan( sessionImplementor.getFactory() );

										// set [, REVEND_TSTMP = ?]
										if ( isRevisionEndTimestampEnabled ) {
											final Object revEndTimestampObj = revisionTimestampGetter.get( revision );
											final Date revisionEndTimestamp = convertRevEndTimestampToDate( revEndTimestampObj );
											final Type revEndTsType = rootAuditedEntityQueryable.getPropertyType(
													configuration.getRevisionEndTimestampFieldName()
											);
											revEndTsType.nullSafeSet(
													preparedStatement, revisionEndTimestamp, index, sessionImplementor
											);
											index += revEndTsType.getColumnSpan( sessionImplementor.getFactory() );
										}

										// where (prod_ent_id) = ?
										final Type idType = rootProductionEntityQueryable.getIdentifierType();
										idType.nullSafeSet( preparedStatement, id, index, sessionImplementor );
										index += idType.getColumnSpan( sessionImplementor.getFactory() );

										// where REV <> ?
										final Type revType = rootAuditedEntityQueryable.getPropertyType(
												configuration.getRevisionNumberPath()
										);
										revType.nullSafeSet( preparedStatement, revisionNumber, index, sessionImplementor );

										// where REVEND is null
										// 		nothing to bind....

										return sessionImplementor
												.getJdbcCoordinator().getResultSetReturn().executeUpdate( preparedStatement );
									}
									finally {
										sessionImplementor.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release(
												preparedStatement
										);
										sessionImplementor.getJdbcCoordinator().afterStatementExecution();
									}
								}
							}
					);

					if ( rowCount != 1 && ( !reuseEntityIdentifier || ( getRevisionType( configuration, data ) != RevisionType.ADD ) ) ) {
						throw new RuntimeException(
								"Cannot update previous revision for entity " + auditedEntityName + " and id " + id
						);
					}
				}
			} );
		}
		sessionCacheCleaner.scheduleAuditDataRemoval( session, data );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			Configuration configuration,
			PersistentCollectionChangeData persistentCollectionChangeData, Object revision) {
		final QueryBuilder qb = new QueryBuilder(
				persistentCollectionChangeData.getEntityName(),
				MIDDLE_ENTITY_ALIAS,
				( (SharedSessionContractImplementor) session ).getFactory()
		);

		final String originalIdPropName = configuration.getOriginalIdPropertyName();
		final Map<String, Object> originalId = (Map<String, Object>) persistentCollectionChangeData.getData().get(
				originalIdPropName
		);
		final String revisionFieldName = configuration.getRevisionFieldName();
		final String revisionTypePropName = configuration.getRevisionTypePropertyName();
		final String ordinalPropName = configuration.getEmbeddableSetOrdinalPropertyName();

		// Adding a parameter for each id component, except the rev number and type.
		for ( Map.Entry<String, Object> originalIdEntry : originalId.entrySet() ) {
			if ( !revisionFieldName.equals( originalIdEntry.getKey() )
					&& !revisionTypePropName.equals( originalIdEntry.getKey() )
					&& !ordinalPropName.equals( originalIdEntry.getKey() ) ) {
				qb.getRootParameters().addWhereWithParam(
						originalIdPropName + "." + originalIdEntry.getKey(),
						true, "=", originalIdEntry.getValue()
				);
			}
		}

		if ( isNonIdentifierWhereConditionsRequired( entityName, propertyName, (SessionImplementor) session ) ) {
			addNonIdentifierWhereConditions( qb, persistentCollectionChangeData.getData(), originalIdPropName );
		}

		addEndRevisionNullRestriction( configuration, qb.getRootParameters() );

		final List<Object> l = qb.toQuery( session ).setLockOptions( LockOptions.UPGRADE ).list();

		// Update the last revision if one exists.
		// HHH-5967: with collections, the same element can be added and removed multiple times. So even if it's an
		// ADD, we may need to update the last revision.
		if ( l.size() > 0 ) {
			updateLastRevision(
					session, configuration, l, originalId, persistentCollectionChangeData.getEntityName(), revision
			);
		}

		// Save the audit data
		session.save( persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData() );
		sessionCacheCleaner.scheduleAuditDataRemoval( session, persistentCollectionChangeData.getData() );
	}

	/**
	 * {@inheritDoc}
	 *
	 * For this implmenetation, the revision-end column is used
	 * <p>
	 * {@code e.revision <= :revision and (e.endRevision > :revision or e.endRevision is null}
	 */
	@Override
	public void addEntityAtRevisionRestriction(
			Configuration configuration,
			QueryBuilder rootQueryBuilder,
			Parameters parameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			MiddleIdData idData,
			String revisionPropertyPath,
			String originalIdPropertyName,
			String alias1,
			String alias2,
			boolean inclusive) {
		addRevisionRestriction( parameters, revisionProperty, revisionEndProperty, addAlias, inclusive );
	}

	/**
	 * {@inheritDoc}
	 *
	 * For this implmenetation, the revision-end column is used
	 * <p>
	 * {@code e.revision <= :revision and (e.endRevision > :revision or e.endRevision is null}
	 */
	@Override
	public void addAssociationAtRevisionRestriction(
			QueryBuilder rootQueryBuilder,
			Parameters parameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			MiddleIdData referencingIdData,
			String versionsMiddleEntityName,
			String eeOriginalIdPropertyPath,
			String revisionPropertyPath,
			String originalIdPropertyName,
			String alias1,
			boolean inclusive,
			MiddleComponentData... componentDatas) {
		addRevisionRestriction( parameters, revisionProperty, revisionEndProperty, addAlias, inclusive );
	}

	/**
	 * @deprecated since 5.4 with no replacement.
	 */
	@Deprecated
	public void setRevisionTimestampGetter(Getter revisionTimestampGetter) {
		this.revisionTimestampGetter = revisionTimestampGetter;
	}

	private void addRevisionRestriction(
			Parameters rootParameters, String revisionProperty, String revisionEndProperty,
			boolean addAlias, boolean inclusive) {
		// e.revision <= _revision and (e.endRevision > _revision or e.endRevision is null)
		Parameters subParm = rootParameters.addSubParameters( "or" );
		rootParameters.addWhereWithNamedParam( revisionProperty, addAlias, inclusive ? "<=" : "<", REVISION_PARAMETER );
		subParm.addWhereWithNamedParam(
				revisionEndProperty + ".id", addAlias, inclusive ? ">" : ">=", REVISION_PARAMETER
		);
		subParm.addWhere( revisionEndProperty, addAlias, "is", "null", false );
	}

	@SuppressWarnings({"unchecked"})
	private RevisionType getRevisionType(Configuration configuration, Object data) {
		return (RevisionType) ( (Map<String, Object>) data ).get( configuration.getRevisionTypePropertyName() );
	}

	@SuppressWarnings({"unchecked"})
	private void updateLastRevision(
			Session session,
			Configuration configuration,
			List<Object> l,
			Object id,
			String auditedEntityName,
			Object revision) {
		// There should be one entry
		if ( l.size() == 1 ) {
			// Setting the end revision to be the current rev
			Object previousData = l.get( 0 );
			String revisionEndFieldName = configuration.getRevisionEndFieldName();
			( (Map<String, Object>) previousData ).put( revisionEndFieldName, revision );

			if ( configuration.isRevisionEndTimestampEnabled() ) {
				// Determine the value of the revision property annotated with @RevisionTimestamp
				String revEndTimestampFieldName = configuration.getRevisionEndTimestampFieldName();
				Object revEndTimestampObj = this.revisionTimestampGetter.get( revision );
				Date revisionEndTimestamp = convertRevEndTimestampToDate( revEndTimestampObj );

				// Setting the end revision timestamp
				( (Map<String, Object>) previousData ).put( revEndTimestampFieldName, revisionEndTimestamp );
			}

			// Saving the previous version
			session.save( auditedEntityName, previousData );
			sessionCacheCleaner.scheduleAuditDataRemoval( session, previousData );
		}
		else {
			throw new RuntimeException( "Cannot find previous revision for entity " + auditedEntityName + " and id " + id );
		}
	}

	private Date convertRevEndTimestampToDate(Object revEndTimestampObj) {
		// convert to a java.util.Date
		if ( revEndTimestampObj instanceof Date ) {
			return (Date) revEndTimestampObj;
		}
		return new Date( (Long) revEndTimestampObj );
	}

	private Queryable getQueryable(String entityName, SessionImplementor sessionImplementor) {
		return (Queryable) sessionImplementor.getFactory().getMetamodel().entityPersister( entityName );
	}

	private void addEndRevisionNullRestriction(Configuration configuration, Parameters rootParameters) {
		rootParameters.addWhere( configuration.getRevisionEndFieldName(), true, "is", "null", false );
	}

	private void addNonIdentifierWhereConditions(QueryBuilder qb, Map<String, Object> data, String originalIdPropertyName) {
		final Parameters parameters = qb.getRootParameters();
		for ( Map.Entry<String, Object> entry : data.entrySet() ) {
			if ( !originalIdPropertyName.equals( entry.getKey() ) ) {
				if ( entry.getValue() != null ) {
					parameters.addWhereWithParam( entry.getKey(), true, "=", entry.getValue() );
				}
				else {
					parameters.addNullRestriction( entry.getKey(), true );
				}
			}
		}
	}

	private boolean isNonIdentifierWhereConditionsRequired(String entityName, String propertyName, SessionImplementor session) {
		final Type propertyType = session.getSessionFactory().getMetamodel().entityPersister( entityName ).getPropertyType( propertyName );
		if ( propertyType.isCollectionType() ) {
			final CollectionType collectionType = (CollectionType) propertyType;
			final Type collectionElementType = collectionType.getElementType( session.getSessionFactory() );
			if ( collectionElementType instanceof ComponentType ) {
				// required for Embeddables
				return true;
			}
			else if ( isMaterializedClob( collectionElementType ) ) {
				// for Map<> using @Lob annotations
				return collectionType instanceof MapType;
			}
		}
		return false;
	}

	private boolean isMaterializedClob(Type collectionElementType) {
		if ( collectionElementType instanceof BasicType<?> ) {
			final BasicType<?> basicType = (BasicType<?>) collectionElementType;
			return basicType.getJavaType() == String.class && (
					basicType.getJdbcTypeDescriptor().getJdbcTypeCode() == Types.CLOB
							|| basicType.getJdbcTypeDescriptor().getJdbcTypeCode() == Types.NCLOB
			);
		}
		return false;
	}
}
