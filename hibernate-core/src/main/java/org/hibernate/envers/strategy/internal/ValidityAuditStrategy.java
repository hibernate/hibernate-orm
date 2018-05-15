/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy.internal;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.boot.spi.AuditServiceOptions;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.spi.MappingContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceStrategy;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.dom4j.Element;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
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
	public void addAdditionalColumns(MappingContext mappingContext) {
		// Add the end-revision field, if the appropriate strategy is used.

		Element endRevMapping = (Element) mappingContext.getRevisionEntityMapping().clone();

		endRevMapping.setName( "many-to-one" );
		endRevMapping.addAttribute( "name", mappingContext.getOptions().getRevisionEndFieldName() );
		MetadataTools.addOrModifyColumn( endRevMapping, mappingContext.getOptions().getRevisionEndFieldName() );

		mappingContext.getAuditEntityMapping().add( endRevMapping );

		if ( mappingContext.getOptions().isRevisionEndTimestampEnabled() ) {
			// add a column for the timestamp of the end revision
			final Element timestampProperty = MetadataTools.addProperty(
					mappingContext.getAuditEntityMapping(),
					mappingContext.getOptions().getRevisionEndTimestampFieldName(),
					getTimestampTypeName( mappingContext ),
					true,
					true,
					false
			);
			MetadataTools.addColumn(
					timestampProperty,
					mappingContext.getOptions().getRevisionEndTimestampFieldName(),
					null,
					null,
					null,
					null,
					null,
					null
			);
		}
	}

	@Override
	public void postInitialize(Class<?> revisionInfoClass, PropertyData timestampData, ServiceRegistry serviceRegistry) {
		// further initialization required
		final Getter revisionTimestampGetter = ReflectionTools.getGetter(
				revisionInfoClass,
				timestampData,
				serviceRegistry
		);
		setRevisionTimestampGetter( revisionTimestampGetter );
	}

	@Override
	public void perform(
			final Session session,
			final String entityName,
			final AuditService auditService,
			final Object id,
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
					sessionImplementor -> {
						final BasicExecutionContext executionContext = new BasicExecutionContext( sessionImplementor );

						// construct the update statements
						final List<SqlAstUpdateDescriptor> updateDescriptors = getUpdateDescriptors(
								entityName,
								auditedEntityName,
								sessionImplementor,
								auditService,
								id,
								revision
						);

						if ( updateDescriptors.isEmpty() ) {
							throw new AuditException(
									String.format(
											Locale.ROOT,
											"Failed to build update statements for entity %s and id %s",
											auditedEntityName,
											id
									)
							);
						}

						// execute the statements
						for ( SqlAstUpdateDescriptor updateDescriptor : updateDescriptors ) {
							final int rowsAffected = JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute(
									UpdateToJdbcUpdateConverter.createJdbcUpdate(
											updateDescriptor.getSqlAstStatement(),
											executionContext.getSession().getSessionFactory()
									),
									executionContext,
									Connection::prepareStatement
							);

							if ( rowsAffected != 1 ) {
								final RevisionType revisionType = getRevisionType( options, data );
								if ( !reuseIdentifierNames || revisionType != RevisionType.ADD ) {
									throw new AuditException(
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

		final QueryBuilder qb = new QueryBuilder(
				persistentCollectionChangeData.getEntityName(),
				MIDDLE_ENTITY_ALIAS,
				( (SharedSessionContractImplementor) session ).getFactory()
		);

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
		final EntityTypeDescriptor<Object> entityDescriptor = sessionFactory.getMetamodel().findEntityDescriptor( entityName );
		entityDescriptor.visitAttributes(
				attribute -> {
					if ( attribute.getName().equals( propertyName )
							&& BagPersistentAttribute.class.isInstance( attribute ) ) {
						// Handling collection of components.
						if ( ( (PluralPersistentAttribute) attribute ).getElementType() instanceof javax.persistence.metamodel.EmbeddableType ) {
							// Adding restrictions to compare data outside of primary key.
							// todo: is it necessary that non-primary key attributes be compared?
							for ( Map.Entry<String, Object> dataEntry : persistentCollectionChangeData.getData()
									.entrySet() ) {
								if ( !originalIdPropName.equals( dataEntry.getKey() ) ) {
									if ( dataEntry.getValue() != null ) {
										qb.getRootParameters().addWhereWithParam(
												dataEntry.getKey(),
												true,
												"=",
												dataEntry.getValue()
										);
									}
									else {
										qb.getRootParameters().addNullRestriction( dataEntry.getKey(), true );
									}
								}
							}
						}
					}
				}
		);
		addEndRevisionNullRestriction( options, qb.getRootParameters() );

		final List<Object> l = qb.toQuery( (SharedSessionContractImplementor) session ).setLockOptions( LockOptions.UPGRADE ).list();

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
		subParm.addWhereWithNamedParam(
				revisionEndProperty + ".id",
				addAlias,
				inclusive ? ">" : ">=",
				REVISION_PARAMETER
		);
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

	private List<SqlAstUpdateDescriptor> getUpdateDescriptors(
			String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Object id,
			Object revision) {
		final AuditServiceOptions options = auditService.getOptions();

		IdentifiableTypeDescriptor entityDescriptor = getEntityDescriptor( entityName, session );

		List<SqlAstUpdateDescriptor> statements = new ArrayList<>();

		// HHH-9062 - update inherited
		if ( options.isRevisionEndTimestampEnabled() && !options.isRevisionEndTimestampLegacyBehaviorEnabled() ) {
			if ( entityDescriptor.getHierarchy().getInheritanceStrategy().equals( InheritanceStrategy.JOINED ) ) {
				while ( entityDescriptor.getSuperclassType() != null ) {
					statements.add(
							buildNonRootEntityUpdateDescriptor(
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
			}
		}

		// process root entity
		statements.add(
				buildUpdateDescriptor(
						entityName,
						auditedEntityName,
						session,
						auditService,
						id,
						revision
				)
		);

		return statements;
	}

	private SqlAstUpdateDescriptor buildUpdateDescriptor(
			String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Object id,
			Object revision) {

		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();

		final EntityTypeDescriptor entityDescriptor = getEntityDescriptor( entityName, session );
		final EntityTypeDescriptor rootEntityDescriptor = entityDescriptor.getHierarchy().getRootEntityType();
		final EntityTypeDescriptor auditedEntityDescriptor = getEntityDescriptor( auditedEntityName, session );
		final EntityTypeDescriptor rootAuditedEntityDescriptor = auditedEntityDescriptor.getHierarchy().getRootEntityType();

		final AuditServiceOptions options = auditService.getOptions();

		final TableReference tableReference = getUpdateTableReference(
				rootEntityDescriptor,
				rootAuditedEntityDescriptor,
				auditedEntityDescriptor
		);

		// The expected output from this method is an UPDATE statement that follows:
		// UPDATE audit_ent
		//	SET REVEND = ?
		//	[, REVEND_TSTMP = ?]
		//	WHERE (entity_id) = ?
		//	AND REV <> ?
		//	AND REVEND is null

		final UpdateStatement.UpdateStatementBuilder builder = new UpdateStatement.UpdateStatementBuilder(
				tableReference
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET REVEND = ?
		final Number revisionNumber = auditService.getRevisionInfoNumberReader().getRevisionNumber( revision );
		rootAuditedEntityDescriptor.findPersistentAttribute( options.getRevisionEndFieldName() ).dehydrate(
				revisionNumber,
				(jdbcValue, type, boundColumn) -> {
					if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
						builder.addAssignment(
								new Assignment(
										new ColumnReference( boundColumn ),
										new LiteralParameter(
												jdbcValue,
												boundColumn.getExpressableType(),
												Clause.UPDATE,
												session.getFactory().getTypeConfiguration()
										)
								)
						);
					}
				},
				Clause.UPDATE,
				session
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// [, REVEND_TSTMP = ?]
		if ( options.isRevisionEndTimestampEnabled() ) {
			rootAuditedEntityDescriptor.findPersistentAttribute( options.getRevisionEndTimestampFieldName() ).dehydrate(
					getRevisionEndTimestampValue( revision, options ),
					(jdbcValue, type, boundColumn) -> {
						if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
							builder.addAssignment(
									new Assignment(
											new ColumnReference( boundColumn ),
											new LiteralParameter(
													jdbcValue,
													boundColumn.getExpressableType(),
													Clause.UPDATE,
													session.getFactory().getTypeConfiguration()
											)
									)
							);
						}
					},
					Clause.UPDATE,
					session
			);
		}

		applyUpdateWhereCommon(
				builder,
				rootEntityDescriptor,
				rootAuditedEntityDescriptor,
				options,
				session,
				id,
				revisionNumber
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REVEND is NULL
		auditedEntityDescriptor.findPersistentAttribute( options.getRevisionEndFieldName() ).dehydrate(
				null,
				(jdbcValue, type, boundColumn) -> {
					builder.addRestriction(
							new NullnessPredicate(
									new ColumnReference( boundColumn ),
									false
							)
					);
				},
				Clause.WHERE,
				session
		);

		return builder.createUpdateDescriptor();
	}

	private SqlAstUpdateDescriptor buildNonRootEntityUpdateDescriptor(
			String entityName,
			String auditedEntityName,
			SessionImplementor session,
			AuditService auditService,
			Object id,
			Object revision) {

		// The expected output from this method is an UPDATE statement that follows:
		// UPDATE audit_ent
		//	SET REVEND_TSTMP = ?
		// WHERE (entity_id) = ?
		// AND REV <> ?
		// AND REVEND_TSTMP is null

		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final AuditServiceOptions options = auditService.getOptions();

		final EntityTypeDescriptor entityDescriptor = getEntityDescriptor( entityName, session );
		final EntityTypeDescriptor auditedEntityDescriptor = getEntityDescriptor( auditedEntityName, session );

		final TableReference tableReference = getUpdateTableReference(
				entityDescriptor,
				auditedEntityDescriptor,
				auditedEntityDescriptor
		);

		final UpdateStatement.UpdateStatementBuilder builder = new UpdateStatement.UpdateStatementBuilder( tableReference );

		final NonIdPersistentAttribute revisionEndTimestampAttribute = auditedEntityDescriptor.findPersistentAttribute(
				options.getRevisionEndTimestampFieldName()
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SET REVEND_TSTMP = ?
		revisionEndTimestampAttribute.dehydrate(
				getRevisionEndTimestampValue( revision, options ),
				(jdbcValue, type, boundColumn) -> {
					builder.addAssignment(
							new Assignment(
									new ColumnReference( boundColumn ),
									new LiteralParameter(
											jdbcValue,
											boundColumn.getExpressableType(),
											Clause.UPDATE,
											session.getFactory().getTypeConfiguration()
									)
							)
					);
				},
				Clause.UPDATE,
				session
		);

		applyUpdateWhereCommon(
				builder,
				entityDescriptor,
				auditedEntityDescriptor,
				options,
				session,
				id,
				auditService.getRevisionInfoNumberReader().getRevisionNumber( revision )
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REVEND_TSTMP is null
		revisionEndTimestampAttribute.dehydrate(
				null,
				(jdbcValue, type, boundColumn) -> {
					builder.addRestriction( new NullnessPredicate( new ColumnReference( boundColumn ), false ) );
				},
				Clause.WHERE,
				session
		);

		return builder.createUpdateDescriptor();
	}

	private void applyUpdateWhereCommon(
			UpdateStatement.UpdateStatementBuilder builder,
			EntityTypeDescriptor entityDescriptor,
			EntityTypeDescriptor auditedEntityDescriptor,
			AuditServiceOptions options,
			SessionImplementor session,
			Object id,
			Number revisionNumber) {

		final EntityIdentifier productionIdentifier = entityDescriptor.getHierarchy().getIdentifierDescriptor();

		// WHERE (entity_id) = ?
		Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
		productionIdentifier.dehydrate(
				productionIdentifier.unresolve( id, session ),
				(jdbcValue, type, boundColumn) -> {
					identifierJunction.add(
							new RelationalPredicate(
									RelationalPredicate.Operator.EQUAL,
									new ColumnReference( boundColumn ),
									new LiteralParameter(
											jdbcValue,
											boundColumn.getExpressableType(),
											Clause.WHERE,
											session.getFactory().getTypeConfiguration()
									)
							)
					);
				},
				Clause.WHERE,
				session
		);
		builder.addRestriction( identifierJunction );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// AND REV <> ?
		// In this case we want the REV field which is part of the identifier.
		// We visit the identifier columns until we locate it and then apply the predicate.
		auditedEntityDescriptor.getIdentifierDescriptor().visitColumns(
				new BiConsumer<SqlExpressableType, Column>() {
					@Override
					public void accept(SqlExpressableType sqlExpressableType, Column column) {
						if ( column.getExpression().equals( options.getRevisionFieldName() ) ) {
							builder.addRestriction(
									new RelationalPredicate(
											RelationalPredicate.Operator.NOT_EQUAL,
											new ColumnReference( column ),
											new LiteralParameter(
													revisionNumber,
													column.getExpressableType(),
													Clause.WHERE,
													session.getFactory().getTypeConfiguration()
											)
									)
							);
						}
					}
				},
				Clause.IRRELEVANT,
				session.getFactory().getTypeConfiguration()
		);
	}

	private EntityTypeDescriptor getEntityDescriptor(String entityName, SessionImplementor sessionImplementor) {
		return sessionImplementor.getFactory().getMetamodel().findEntityDescriptor( entityName );
	}

	private TableReference getUpdateTableReference(
			EntityTypeDescriptor rootEntityDescriptor,
			EntityTypeDescriptor rootAuditedEntityDescriptor,
			EntityTypeDescriptor auditedEntityDescriptor) {
		if ( rootEntityDescriptor.getHierarchy().getInheritanceStrategy().equals( InheritanceStrategy.UNION ) ) {
			// this is the condition causing all the problems in terms of the generated SQL UPDATE
			// the problem being that we currently try to update the in-line view made up of the union query
			//
			// this is extremely hacky means to get the root table name for the union subclass style entities.
			// hacky because it relies on internal behavior of UnionSubclassEntityPersister
			// !!!!!! NOTICE - using subclass persister, not root !!!!!!
			return new TableReference( auditedEntityDescriptor.getPrimaryTable(), null , false);
		}
		return new TableReference( rootAuditedEntityDescriptor.getPrimaryTable(), null, false );
	}

	private String getTimestampTypeName(MappingContext mappingContext) {
		if ( mappingContext.getOptions().isNumericRevisionEndTimestampEnabled() ) {
			return getBasicTypeSqlType( mappingContext.getDialect(), StandardSpiBasicTypes.LONG );
		}
		return getBasicTypeSqlType( mappingContext.getDialect(), StandardSpiBasicTypes.TIMESTAMP );
	}

	private String getBasicTypeSqlType(Dialect dialect, BasicType basicType) {
		return dialect.getTypeName( basicType.getSqlTypeDescriptor().getJdbcTypeCode() );
	}
}
