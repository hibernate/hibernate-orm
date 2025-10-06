/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.AssertionFailure;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.sql.exec.SqlExecLogger.SQL_EXEC_LOGGER;


/**
 * Models a single table for which to apply locking.
 *
 * @author Steve Ebersole
 */
public class TableLock {
	private final TableDetails tableDetails;
	private final EntityMappingType entityMappingType;

	private final QuerySpec querySpec = new QuerySpec( true );

	private final NavigablePath rootPath;

	private final TableReference physicalTableReference;
	private final TableGroup physicalTableGroup;

	private final TableReference logicalTableReference;
	private final TableGroup logicalTableGroup;

	private final LockingCreationStates creationStates;

	private final List<ResultHandler> resultHandlers = new ArrayList<>();
	private final List<DomainResult<?>> domainResults = new ArrayList<>();

	private final JdbcParameterBindings jdbcParameterBindings;

	public TableLock(
			TableDetails tableDetails,
			EntityMappingType entityMappingType,
			List<EntityKey> entityKeys,
			SharedSessionContractImplementor session) {
		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.debugf( "Adding table `%s` for follow-on locking - %s", tableDetails.getTableName(), entityMappingType.getEntityName() );
		}

		this.tableDetails = tableDetails;
		this.entityMappingType = entityMappingType;

		rootPath = new NavigablePath( tableDetails.getTableName() );
		physicalTableReference =
				new NamedTableReference( tableDetails.getTableName(), "tbl" );
		physicalTableGroup =
				new LockingTableGroup(
						physicalTableReference,
						tableDetails.getTableName(),
						entityMappingType,
						tableDetails.getKeyDetails()
				);

		if ( entityMappingType.getEntityPersister() instanceof UnionSubclassEntityPersister usp ) {
			final var unionTableReference = new UnionTableReference(
					tableDetails.getTableName(),
					usp.getSynchronizedQuerySpaces(),
					"tbl",
					false
			);
			this.logicalTableReference = unionTableReference;
			this.logicalTableGroup = new UnionTableGroup(
					true,
					rootPath,
					unionTableReference,
					usp,
					null
			);
		}
		else {
			logicalTableReference = physicalTableReference;
			logicalTableGroup = physicalTableGroup;
		}

		querySpec.getFromClause().addRoot( physicalTableGroup );

		creationStates = new LockingCreationStates(
				querySpec,
				logicalTableGroup,
				entityMappingType.getEntityPersister().getFactory()
		);

		// add the key as the first result
		domainResults.add( tableDetails.getKeyDetails().createDomainResult(
				rootPath.append( "{key}" ),
				logicalTableReference,
				null,
				creationStates
		) );

		final int expectedParamCount =
				entityKeys.size() * entityMappingType.getIdentifierMapping().getJdbcTypeCount();
		jdbcParameterBindings = new JdbcParameterBindingsImpl( expectedParamCount );

		applyKeyRestrictions( entityKeys, session );
	}

	public void applyAttribute(int index, AttributeMapping attributeMapping) {
		final var attributePath = rootPath.append( attributeMapping.getPartName() );
		final DomainResult<Object> domainResult;
		final ResultHandler resultHandler;
		if ( attributeMapping instanceof ToOneAttributeMapping toOne ) {
			domainResult =
					toOne.getForeignKeyDescriptor().getKeyPart()
							.createDomainResult(
									attributePath,
									logicalTableGroup,
									ForeignKeyDescriptor.PART_NAME,
									creationStates
							);
			resultHandler = new ToOneResultHandler( index, toOne );
		}
		else {
			domainResult =
					attributeMapping.createDomainResult(
							attributePath,
							logicalTableGroup,
							null,
							creationStates
					);
			resultHandler = new NonToOneResultHandler( index );
		}
		domainResults.add( domainResult );
		resultHandlers.add( resultHandler );
	}

	public void applyKeyRestrictions(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		// todo (JdbcOperation) : Consider leveraging approach based on Dialect#useArrayForMultiValuedParameters
		if ( entityMappingType.getIdentifierMapping().getJdbcTypeCount() == 1 ) {
			applySimpleKeyRestriction( entityKeys, session );
		}
		else {
			applyCompositeKeyRestriction( entityKeys, session );
		}
	}

	private void applySimpleKeyRestriction(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		final var identifierMapping = entityMappingType.getIdentifierMapping();

		final var keyColumn = tableDetails.getKeyDetails().getKeyColumn( 0 );
		final var columnReference = new ColumnReference( physicalTableReference, keyColumn );

		final var restriction = new InListPredicate( columnReference );
		querySpec.applyPredicate( restriction );

		entityKeys.forEach( (entityKey) -> identifierMapping.breakDownJdbcValues(
				entityKey.getIdentifierValue(),
				(valueIndex, value, jdbcValueMapping) -> {
					final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
					final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
					restriction.addExpression( jdbcParameter );
					jdbcParameterBindings.addBinding( jdbcParameter,
							new JdbcParameterBindingImpl( jdbcMapping, value ) );
				},
				session
		) );
	}

	private void applyCompositeKeyRestriction(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		final var identifierMapping = entityMappingType.getIdentifierMapping();

		final ArrayList<ColumnReference> columnRefs = arrayList( tableDetails.getKeyDetails().getColumnCount() );
		tableDetails.getKeyDetails().forEachKeyColumn( (position, keyColumn) -> {
			columnRefs.add( new ColumnReference( physicalTableReference, keyColumn ) );
		} );
		final var keyRef = new SqlTuple( columnRefs, identifierMapping );

		final var restriction = new InListPredicate( keyRef );
		querySpec.applyPredicate( restriction );

		entityKeys.forEach( (entityKey) -> {
			final List<JdbcParameterImpl> valueParams = arrayList( tableDetails.getKeyDetails().getColumnCount() );
			identifierMapping.breakDownJdbcValues(
					entityKey.getIdentifierValue(),
					(valueIndex, value, jdbcValueMapping) -> {
						final var jdbcMapping = jdbcValueMapping.getJdbcMapping();
						final var jdbcParameter = new JdbcParameterImpl( jdbcMapping );
						valueParams.add( jdbcParameter );
						jdbcParameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, value ) );
					},
					session
			);
			final var valueTuple = new SqlTuple( valueParams, identifierMapping );
			restriction.addExpression( valueTuple );
		} );
	}

	public void performActions(Map<Object, EntityDetails> entityDetailsMap, QueryOptions lockingQueryOptions, SharedSessionContractImplementor session) {
		final var sessionFactory = session.getSessionFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var selectStatement = new SelectStatement( querySpec, domainResults );
		final List<Object[]> results =
				jdbcServices.getJdbcSelectExecutor()
						.executeQuery(
								jdbcServices.getDialect().getSqlAstTranslatorFactory()
										.buildSelectTranslator( sessionFactory, selectStatement )
										.translate( jdbcParameterBindings, lockingQueryOptions ),
								jdbcParameterBindings,
								// IMPORTANT: we need a "clean" ExecutionContext to not further apply locking
								new BaseExecutionContext( session ),
								row -> row,
								Object[].class,
								StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
								ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.ALLOW )
						);

		if ( isEmpty( results ) ) {
			throw new AssertionFailure( "Expecting results" );
		}

		results.forEach( (row) -> {
			final Object id = row[0];
			final var entityDetails = entityDetailsMap.get( id );
			for ( int i = 0; i < resultHandlers.size(); i++ ) {
				// offset 1 because of the id at position 0
				resultHandlers.get( i ).applyResult( row[i+1], entityDetails, session );
			}
		} );
	}


	private interface ResultHandler {
		void applyResult(Object state, EntityDetails entityDetails, SharedSessionContractImplementor session);
	}

	private static abstract class AbstractResultHandler implements ResultHandler {
		protected final Integer statePosition;

		public AbstractResultHandler(Integer statePosition) {
			this.statePosition = statePosition;
		}
	}

	private static class NonToOneResultHandler extends AbstractResultHandler {
		public NonToOneResultHandler(Integer statePosition) {
			super( statePosition );
		}

		@Override
		public void applyResult(Object stateValue, EntityDetails entityDetails, SharedSessionContractImplementor session) {
			applyLoadedState( entityDetails, statePosition, stateValue );
			applyModelState( entityDetails, statePosition, stateValue );
		}
	}

	private static class ToOneResultHandler extends AbstractResultHandler {
		private final ToOneAttributeMapping toOne;

		public ToOneResultHandler(Integer statePosition, ToOneAttributeMapping toOne) {
			super( statePosition );
			this.toOne = toOne;
		}

		@Override
		public void applyResult(Object stateValue, EntityDetails entityDetails, SharedSessionContractImplementor session) {
			final Object reference;
			if ( stateValue == null ) {
				if ( !toOne.isNullable() ) {
					throw new IllegalStateException( "Retrieved key was null, but to-one is not nullable : " + toOne.getNavigableRole().getFullPath() );
				}
				else {
					reference = null;
				}
			}
			else {
				reference = session.internalLoad(
						toOne.getAssociatedEntityMappingType().getEntityName(),
						stateValue,
						false,
						toOne.isNullable()
				);
			}
			applyLoadedState( entityDetails, statePosition, reference );
			applyModelState( entityDetails, statePosition, reference );
		}
	}

	private static void applyLoadedState(EntityDetails entityDetails, Integer statePosition, Object stateValue) {
		final var entry = entityDetails.entry();
		final var loadedState = entry.getLoadedState();
		if ( loadedState != null ) {
			loadedState[statePosition] = stateValue;
		}
		else {
			if ( !entry.isReadOnly() ) {
				throw new AssertionFailure( "Expecting entity entry to be read-only - " + entityDetails.instance() );
			}
		}
	}

	private static void applyModelState(EntityDetails entityDetails, Integer statePosition, Object reference) {
		entityDetails.key().getPersister().getAttributeMapping( statePosition ).setValue( entityDetails.instance(), reference );
	}
}
