/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.boot.spi.AuditServiceOptions;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.sql.Update;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.spi.WrapperOptions;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Audit strategy which persists and retrieves audit information using a validity algorithm, based on the
 * start-revision and end-revision of a row in the audit tables.
 * <p>This algorithm works as follows:
 * <ul>
 * <li>For a <strong>new row</strong> that is persisted in an audit table, only the <strong>start-revision</strong> column of that row is set</li>
 * <li>At the same time the <strong>end-revision</strong> field of the <strong>previous</strong> audit row is set to this revision</li>
 * <li>Queries are retrieved using 'between start and end revision', instead of a subquery.</li>
 * </ul>
 * </p>
 * <p/>
 * <p>
 * This has a few important consequences that need to be judged against against each other:
 * <ul>
 * <li>Persisting audit information is a bit slower, because an extra row is updated</li>
 * <li>Retrieving audit information is a lot faster</li>
 * </ul>
 * </p>
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
	public void perform(
			final Session session,
			final String entityName,
			final AuditService auditService,
			final Serializable id,
			final Object data,
			final Object revision) {
		final String auditedEntityName = auditService.getAuditEntityName( entityName );
		final AuditServiceOptions options = auditService.getOptions();

		// When application reuses identifiers of previously removed entities:
		// The UPDATE statement will no-op if an entity with a given identifier has been
		// inserted for the first time. But in case a deleted primary key value was
		// reused, this guarantees correct strategy behavior: exactly one row with
		// null end date exists for each identifier.
		final boolean reuseIdentifierNames = options.isAllowIdentifierReuseEnabled();

		// Save the audit data
		session.save( auditedEntityName, data );

		// Update the end date of the previous row.
		if ( reuseIdentifierNames || getRevisionType( options, data ) != RevisionType.ADD ) {
			// Register transaction completion process to guarantee execution of UPDATE statement after INSERT.
			( (EventSource) session ).getActionQueue().registerProcess(
					new BeforeTransactionCompletionProcess() {
						@Override
						public void doBeforeTransactionCompletion(final SessionImplementor sessionImplementor) {
							// construct the update contexts.
							final List<UpdateContext> updateContexts = getUpdateContexts(
									entityName,
									auditedEntityName,
									sessionImplementor,
									auditService,
									id,
									revision
							);

							if ( updateContexts.isEmpty() ) {
								throw new RuntimeException(
										String.format(
												Locale.ROOT,
												"Failed to build update contexts for entity %s and id %s",
												auditedEntityName,
												id
										)
								);
							}

							// execute the update(s)
							for ( UpdateContext updateContext : updateContexts ) {
								if ( executeUpdate( sessionImplementor, updateContext ) != 1 ) {
									final RevisionType revisionType = getRevisionType( options, data );
									if ( !reuseIdentifierNames || revisionType != RevisionType.ADD ) {
										throw new RuntimeException(
												String.format(
														"Cannot update previous revision for entity %s and id %s",
														auditedEntityName,
														id
												)
										);
									}
								}
							}
						}
					}
			);
		}

		sessionCacheCleaner.scheduleAuditDataRemoval( session, data );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			AuditService auditService,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision) {
		final AuditServiceOptions options = auditService.getOptions();

		final QueryBuilder qb = new QueryBuilder( persistentCollectionChangeData.getEntityName(), MIDDLE_ENTITY_ALIAS );

		final String originalIdPropName = options.getOriginalIdPropName();
		final Map<String, Object> originalId = (Map<String, Object>) persistentCollectionChangeData.getData().get(
				originalIdPropName
		);
		final String revisionFieldName = options.getRevisionFieldName();
		final String revisionTypePropName = options.getRevisionTypePropName();
		final String ordinalPropName = options.getEmbeddableSetOrdinalPropertyName();

		// Adding a parameter for each id component, except the rev number and type.
		for ( Map.Entry<String, Object> originalIdEntry : originalId.entrySet() ) {
			if ( !revisionFieldName.equals( originalIdEntry.getKey() )
					&& !revisionTypePropName.equals( originalIdEntry.getKey() )
					&& !ordinalPropName.equals( originalIdEntry.getKey() ) ) {
				qb.getRootParameters().addWhereWithParam(
						originalIdPropName + "." + originalIdEntry.getKey(),
						true,
						"=",
						originalIdEntry.getValue()
				);
			}
		}

		final SessionFactoryImplementor sessionFactory = ( (SessionImplementor) session ).getFactory();
		final EntityDescriptor<Object> entityDescriptor = sessionFactory.getTypeConfiguration().findEntityDescriptor(
				entityName );
		final Type propertyType = entityDescriptor.getPropertyType( propertyName );
		if ( propertyType.getClassification() ==  Type.Classification.COLLECTION ) {
			final PluralPersistentAttribute attribute = (PluralPersistentAttribute) entityDescriptor.getAttribute(
					propertyName );
			// Handling collection of components.
			if ( attribute.getElementType() instanceof javax.persistence.metamodel.EmbeddableType ) {
				// Adding restrictions to compare data outside of primary key.
				// todo: is it necessary that non-primary key attributes be compared?
				for ( Map.Entry<String, Object> dataEntry : persistentCollectionChangeData.getData().entrySet() ) {
					if ( !originalIdPropName.equals( dataEntry.getKey() ) ) {
						if ( dataEntry.getValue() != null ) {
							qb.getRootParameters().addWhereWithParam( dataEntry.getKey(), true, "=", dataEntry.getValue() );
						}
						else {
							qb.getRootParameters().addNullRestriction( dataEntry.getKey(), true );
						}
					}
				}
			}
		}

		addEndRevisionNullRestriction( options, qb.getRootParameters() );

		final List<Object> l = qb.toQuery( session ).setLockOptions( LockOptions.UPGRADE ).list();

		// Update the last revision if one exists.
		// HHH-5967: with collections, the same element can be added and removed multiple times. So even if it's an
		// ADD, we may need to update the last revision.
		if ( l.size() > 0 ) {
			updateLastRevision(
					session, options, l, originalId, persistentCollectionChangeData.getEntityName(), revision
			);
		}

		// Save the audit data
		session.save( persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData() );
		sessionCacheCleaner.scheduleAuditDataRemoval( session, persistentCollectionChangeData.getData() );
	}

	@Override
	public void addEntityAtRevisionRestriction(
			AuditServiceOptions options,
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

	@Override
	public void addAssociationAtRevisionRestriction(
			QueryBuilder rootQueryBuilder,
			Parameters parameters, String revisionProperty,
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

	public void setRevisionTimestampGetter(Getter revisionTimestampGetter) {
		this.revisionTimestampGetter = revisionTimestampGetter;
	}

	private void addEndRevisionNullRestriction(AuditServiceOptions options, Parameters rootParameters) {
		rootParameters.addWhere( options.getRevisionEndFieldName(), true, "is", "null", false );
	}

	private void addRevisionRestriction(
			Parameters rootParameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			boolean inclusive) {
		// e.revision <= _revision and (e.endRevision > _revision or e.endRevision is null)
		Parameters subParm = rootParameters.addSubParameters( "or" );
		rootParameters.addWhereWithNamedParam( revisionProperty, addAlias, inclusive ? "<=" : "<", REVISION_PARAMETER );
		subParm.addWhereWithNamedParam( revisionEndProperty + ".id", addAlias, inclusive ? ">" : ">=", REVISION_PARAMETER );
		subParm.addWhere( revisionEndProperty, addAlias, "is", "null", false );
	}

	@SuppressWarnings({"unchecked"})
	private RevisionType getRevisionType(AuditServiceOptions options, Object data) {
		return (RevisionType) ( (Map<String, Object>) data ).get( options.getRevisionTypePropName() );
	}

	@SuppressWarnings({"unchecked"})
	private void updateLastRevision(
			Session session,
			AuditServiceOptions options,
			List<Object> l,
			Object id,
			String auditedEntityName,
			Object revision) {
		// There should be one entry
		if ( l.size() == 1 ) {
			// Setting the end revision to be the current rev
			Object previousData = l.get( 0 );
			String revisionEndFieldName = options.getRevisionEndFieldName();
			( (Map<String, Object>) previousData ).put( revisionEndFieldName, revision );

			if ( options.isRevisionEndTimestampEnabled() ) {
				// Determine the value of the revision property annotated with @RevisionTimestamp
				String revEndTimestampFieldName = options.getRevisionEndTimestampFieldName();
				// Setting the end revision timestamp
				( (Map<String, Object>) previousData ).put(
						revEndTimestampFieldName,
						getRevisionEndTimestampValue( revision, options )
				);
			}

			// Saving the previous version
			session.save( auditedEntityName, previousData );
			sessionCacheCleaner.scheduleAuditDataRemoval( session, previousData );
		}
		else {
			throw new RuntimeException( "Cannot find previous revision for entity " + auditedEntityName + " and id " + id );
		}
	}

	private Object getRevisionEndTimestampValue(Object revision, AuditServiceOptions options) {
		Object value = this.revisionTimestampGetter.get( revision );
		if ( options.isNumericRevisionEndTimestampEnabled() ) {
			if ( Date.class.isInstance( value ) ) {
				return ( (Date) value ).getTime();
			}
			return value;
		}
		else {
			if ( Date.class.isInstance( value ) ) {
				return value;
			}
			else {
				return new Date( (long) value );
			}
		}
	}

	/**
	 * Executes the {@link UpdateContext} within the bounds of the specified {@link SessionImplementor}.
	 *
	 * @param session The session.
	 * @param updateContext The UpdateContext.
	 * @return the number of rows affected.
	 */
	private int executeUpdate(SessionImplementor session, UpdateContext updateContext) {
		final String updateSql = updateContext.toStatementString();
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final PreparedStatement preparedStatement = jdbcCoordinator.getStatementPreparer().prepareStatement( updateSql );
		return session.doReturningWork(
				new ReturningWork<Integer>() {
					@Override
					public Integer execute(Connection connection) throws SQLException {
						try {
							int index = 1;
							for ( QueryParameterBinding binding : updateContext.getBindings() ) {
								index += binding.bind( index, preparedStatement, session );
							}
							return jdbcCoordinator.getResultSetReturn().executeUpdate( preparedStatement );
						}
						finally {
							jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
							jdbcCoordinator.afterStatementExecution();
						}
					}
				}
		);
	}

	/**
	 * Creates at least one, perhaps multiple, {@link UpdateContext}s based on the entity hierarchy of the
	 * audited entity instance type.
	 *
	 * @param entityName The entity name.
	 * @param auditedEntityName The audited entity name.
	 * @param session The session.
	 * @param auditService The AuditService.
	 * @param id The entity identifier.
	 * @param revision The revision entity.
	 *
	 * @return list of {@link UpdateContext}s.  Should always contain a minimum of 1 element.
	 */
	private List<UpdateContext> getUpdateContexts(String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Serializable id,
			Object revision) {
		final AuditServiceOptions options = auditService.getOptions();

		IdentifiableTypeDescriptor entityDescriptor = getEntityDescriptor( entityName, session );

		// HHH-9062 - update inherited
		if ( options.isRevisionEndTimestampEnabled() && !options.isRevisionEndTimestampLegacyBehaviorEnabled() ) {
			// todo (6.0) - verify this is correct
			if ( entityDescriptor.getHierarchy().getInheritanceStrategy().equals( InheritanceStrategy.JOINED ) ) {
				List<UpdateContext> contexts = new ArrayList<>();
				// iterate subclasses from farther descendant up the hierarchy, excluding root
				while ( entityDescriptor.getSuperclassType() != null ) {
					contexts.add(
							getNonRootUpdateContext(
									entityName,
									auditedEntityName,
									session,
									auditService,
									id,
									revision
							)
					);
					entityName = entityDescriptor.getSuperclassType().getNavigableName();
					auditedEntityName = auditService.getAuditEntityName( entityName );
					entityDescriptor = getEntityDescriptor( entityName, session );
				}
				// process root entity
				contexts.add(
						getUpdateContext(
								entityName,
								auditedEntityName,
								session,
								auditService,
								id,
								revision
						)
				);
				return contexts;
			}
		}

		return Collections.singletonList(
				getUpdateContext(
						entityName,
						auditedEntityName,
						session,
						auditService,
						id,
						revision
				)
		);
	}

	/**
	 * Creates the update context used to modify the revision end and potentially timestamp values.
	 *
	 * @param entityName The entity name.
	 * @param auditedEntityName The audited entity name.
	 * @param session The session.
	 * @param auditService The AuditService.
	 * @param id The entity identifier.
	 * @param revision The revision entity.
	 * @return The {@link UpdateContext} instance.
	 */
	private UpdateContext getUpdateContext(String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Serializable id,
			Object revision) {

		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();

		final EntityDescriptor entityDescriptor = getEntityDescriptor( entityName, session );
		final EntityDescriptor rootEntityDescriptor = entityDescriptor.getHierarchy().getRootEntityType();
		final EntityDescriptor auditedEntityDescriptor = getEntityDescriptor( auditedEntityName, session );
		final EntityDescriptor rootAuditedEntityDescriptor = auditedEntityDescriptor.getHierarchy().getRootEntityType();

		final AuditServiceOptions options = auditService.getOptions();
		int index = 1;

		// The expected output from this method is an UPDATE statement that follows:
		// UPDATE audit_ent
		//	SET REVEND = ?
		//	[, REVEND_TSTMP = ?]
		//	WHERE (entity_id) = ?
		//	AND REV <> ?
		//	AND REVEND is null

		// UPDATE audit_ent
		final UpdateContext update = new UpdateContext( sessionFactory );
		update.setTableName( getUpdateTableName( rootEntityDescriptor, rootAuditedEntityDescriptor, auditedEntityDescriptor ) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET REVEND = ?
		final Number revisionNumber = auditService.getRevisionInfoNumberReader().getRevisionNumber( revision );
		final BasicValuedNavigable rootAuditedRevisionEndNavigable = getNavigableByPath( rootAuditedEntityDescriptor, options.getRevisionEndFieldName() );
		update.addColumn( rootAuditedRevisionEndNavigable.getBoundColumn().getExpression() );
		update.bind( revisionNumber, rootAuditedRevisionEndNavigable.getBasicType() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// [, REVEND_TSTMP = ?]
		if ( options.isRevisionEndTimestampEnabled() ) {
			final BasicValuedNavigable rootAuditedRevisionEndTimestampNavigable = getNavigableByPath(
					rootAuditedEntityDescriptor,
					options.getRevisionEndTimestampFieldName()
			);
			update.addColumn( rootAuditedRevisionEndTimestampNavigable.getBoundColumn().getExpression() );
			update.bind( getRevisionEndTimestampValue( revision, options ), rootAuditedRevisionEndTimestampNavigable.getBasicType() );
		}

		applyUpdateContextWhereCommon( update, rootEntityDescriptor, rootAuditedEntityDescriptor, options, id, revisionNumber );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REVEND is null
		final BasicValuedNavigable auditedRevisionEndNavigable = getNavigableByPath( auditedEntityDescriptor, options.getRevisionEndFieldName() );
		update.addWhereColumn( auditedRevisionEndNavigable.getBoundColumn().getExpression(), " is null" );

		return update;
	}

	/**
	 * Creates the update context used to modify the revision end timestamp values for a non root entity.
	 *
	 * IMPL NOTE: This is only used to set the revision end timestamp for joined inheritance non-root entity tables.
	 *
	 * @param entityName The entity name.
	 * @param auditedEntityName The audited entity name.
	 * @param session The session.
	 * @param auditService The AuditService.
	 * @param id The entity identifier.
	 * @param revision The revision entity.
	 *
	 * @return The {@link UpdateContext} instance.
	 */
	private UpdateContext getNonRootUpdateContext(String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Serializable id,
			Object revision) {

		// The expected output from this method is an UPDATE statement that follows:
		// UPDATE audit_ent
		//	SET REVEND_TSTMP = ?
		// WHERE (entity_id) = ?
		// AND REV <> ?
		// AND REVEND_TSTMP is null

		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final AuditServiceOptions options = auditService.getOptions();

		final EntityDescriptor entityDescriptor = getEntityDescriptor( entityName, session );
		final EntityDescriptor auditedEntityDescriptor = getEntityDescriptor( auditedEntityName, session );

		final UpdateContext update = new UpdateContext( sessionFactory );
		update.setTableName( getUpdateTableName( entityDescriptor, auditedEntityDescriptor, auditedEntityDescriptor ) );

		final BasicValuedNavigable auditedRevisionEndTimestampNavigable = getNavigableByPath(
				auditedEntityDescriptor,
				options.getRevisionEndTimestampFieldName()
		);

		final Object timestampValue = getRevisionEndTimestampValue( revision, options );
		update.addColumn( auditedRevisionEndTimestampNavigable.getBoundColumn().getExpression() );
		update.bind( timestampValue, auditedRevisionEndTimestampNavigable.getBasicType() );

		final Number revisionNumber = auditService.getRevisionInfoNumberReader().getRevisionNumber( revision );
		applyUpdateContextWhereCommon( update, entityDescriptor, auditedEntityDescriptor, options, id, revisionNumber );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REVEND_TSTMP is null
		update.addWhereColumn( auditedRevisionEndTimestampNavigable.getBoundColumn().getExpression(), " is null" );

		return update;
	}

	/**
	 * Apply common where predicates to the update context.
	 *
	 * @param updateContext The update context.
	 * @param entityDescriptor The entity descriptor.
	 * @param auditedEntityDescriptor The audited entity descriptor.
	 * @param options The AuditServiceOptions.
	 * @param id The entity identifier.
	 * @param revisionNumber The revision number.
	 */
	private void applyUpdateContextWhereCommon(UpdateContext updateContext,
			EntityDescriptor entityDescriptor,
			EntityDescriptor auditedEntityDescriptor,
			AuditServiceOptions options,
			Serializable id,
			Number revisionNumber) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// WHERE (entity_id) = ? AND REV <> ? AND REVEND is null

		// todo (6.0) - how do we handle identifiers now?
		//		Previously we delegated binding to the Type and this was generalized across all 3 identifier use
		//		cases; however, we presently expose ValueBinder through BasicType; which covers just the simple
		//		identifier use case.  We need to determine how we're to deal with aggregate and non-aggregate
		//		based identifier use cases.
		updateContext.addPrimaryKeyColumns( getEntityDescriptorPrimaryKeyColumnNames( entityDescriptor ) );
		updateContext.bind( id, (AllowableParameterType) entityDescriptor.getHierarchy().getIdentifierDescriptor() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REV <> ? (REV is on the auditedEntityDescriptor)
		final BasicValuedNavigable revisionNumberPath = getNavigableByPath( auditedEntityDescriptor, options.getRevisionNumberPath() );
		updateContext.addWhereColumn( revisionNumberPath.getBoundColumn().getExpression(), " <> ?" );
		updateContext.bind( revisionNumber, revisionNumberPath.getBasicType() );
	}

	private EntityDescriptor getEntityDescriptor(String entityName, SessionImplementor sessionImplementor) {
		return sessionImplementor.getFactory().getTypeConfiguration().findEntityDescriptor( entityName );
	}

	private String[] getEntityDescriptorPrimaryKeyColumnNames(EntityDescriptor entityDescriptor) {
		final List<PhysicalColumn> primaryKeyColumns = entityDescriptor.getPrimaryTable().getPrimaryKey().getColumns();
		final String[] columns = new String[ primaryKeyColumns.size() ];
		for ( int i = 0; i < primaryKeyColumns.size(); ++i ) {
			columns[ i ] = primaryKeyColumns.get( i ).getName().render();
		}
		return columns;
	}

	/**
	 * Get the update table name based on the various queryable instances.
	 *
	 * @param rootEntityDescriptor The root entity descriptor.
	 * @param rootAuditedEntityDescriptor The root audited entity descriptor.
	 * @param auditedEntityDescriptor The audited entity descriptor.
	 * @return the update table name.
	 */
	private String getUpdateTableName(
			EntityDescriptor rootEntityDescriptor,
			EntityDescriptor rootAuditedEntityDescriptor,
			EntityDescriptor auditedEntityDescriptor) {
		// todo (6.0) - Verify this is correct
		if ( rootEntityDescriptor.getHierarchy().getInheritanceStrategy().equals( InheritanceStrategy.UNION ) ) {
			// this is the condition causing all the problems in terms of the generated SQL UPDATE
			// the problem being that we currently try to update the in-line view made up of the union query
			//
			// this is extremely hacky means to get the root table name for the union subclass style entities.
			// hacky because it relies on internal behavior of UnionSubclassEntityPersister
			// !!!!!! NOTICE - using subclass persister, not root !!!!!!
			return auditedEntityDescriptor.getPrimaryTable().getTableExpression();
		}
		else {
			return rootAuditedEntityDescriptor.getPrimaryTable().getTableExpression();
		}
	}

	private BasicValuedNavigable getNavigableByPath(EntityDescriptor entityDescriptor, String propertyPath) {
		// todo: improve this for efficiency.
		// 		we likely can determine these values more efficiently based on configuration options or by caching
		// 		the necessary column references in the EntityConfiguration and looking that up directly here.
		final String[] tokens = StringHelper.split( ".", propertyPath );
		NavigableContainer container = entityDescriptor;
		for ( int i = 0; i < tokens.length; ++i ) {
			final Navigable navigable = container.findNavigable( tokens[i] );
			if ( i + 1 < tokens.length && NavigableContainer.class.isInstance( navigable ) ) {
				container = NavigableContainer.class.cast( navigable );
			}
			else {
				if ( BasicValuedNavigable.class.isInstance( navigable ) ) {
					return (BasicValuedNavigable) navigable;
				}
			}
		}
		throw new AuditException( "Failed to locate BasicValuedNavigable for property path [" + propertyPath + "]." );
	}

	/**
	 * Represents the audit strategy's update.
	 */
	private class UpdateContext extends Update {
		private final List<QueryParameterBinding> bindings = new ArrayList<>();
		private final SessionFactoryImplementor sessionFactory;

		public UpdateContext(SessionFactoryImplementor sessionFactory) {
			super( sessionFactory.getJdbcServices().getDialect() );
			this.sessionFactory = sessionFactory;
		}

		/**
		 * Get the query parameter bindings.
		 *
		 * @return list of query parameter bindings.
		 */
		public List<QueryParameterBinding> getBindings() {
			return bindings;
		}

		/**
		 * Utility method for binding a new query parameter.
		 *
		 * @param value the value to be bound.
		 * @param type the type to be bound.
		 */
		public void bind(Object value, AllowableParameterType type) {
			this.bindings.add( new QueryParameterBinding( value, type ) );
		}
	}

	/**
	 * Represents a query parameter binding.
	 */
	private class QueryParameterBinding {
		private final AllowableParameterType type;
		private final Object value;

		public QueryParameterBinding(Object value, AllowableParameterType type) {
			this.value = value;
			this.type = type;
		}

		/**
		 * Bind the parameter to the provided statement.
		 *
		 * @param index the index to be bound.
		 * @param ps The prepared statement.
		 * @param options The wrapper options
		 * @return the number of column spans based on this binding.
		 * @throws SQLException Thrown if a SQL exception occured.
		 */
		public int bind(int index, PreparedStatement ps, WrapperOptions options) throws SQLException {
			this.type.getValueBinder().bind( ps, value, index, options );
			return 1;
		}
	}

	private class IdentifierStrategy implements NavigableVisitationStrategy {

		private final PreparedStatement ps;
		private final WrapperOptions options;
		private final Object value;
		private int index;
		private final UpdateContext context;

		IdentifierStrategy(PreparedStatement ps, WrapperOptions options, Object value, UpdateContext context) {
			this.ps = ps;
			this.options = options;
			this.value = value;
			this.context = context;
		}

		@Override
		public void visitSimpleIdentifier(EntityIdentifierSimple identifier) {
			// delegate to #visitSingularAttributeBasic?
			identifier.visitNavigable( this );
		}

		@Override
		public void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier) {
			// delegates to #visitsingularAttributeEmbedded?
			identifier.visitNavigable( this );
		}

		@Override
		public void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier) {
			// visit each of the composite identifier navigables
			identifier.visitNavigables( this );
		}

		@Override
		public void visitSingularAttributeBasic(BasicSingularPersistentAttribute attribute) {

		}

		@Override
		public void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {
			// delegate to the embedded-id attributes
			attribute.visitNavigables( this );
		}

		@Override
		public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
			// delegate to the to-one association's identifier
			attribute.getEntityDescriptor().getIdentifierDescriptor().visitNavigable( this );
		}
	}
}
