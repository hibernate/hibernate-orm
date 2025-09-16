/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.AssertionFailure;
import org.hibernate.ScrollMode;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.sql.exec.SqlExecLogger.SQL_EXEC_LOGGER;


/**
 * Consolidates processing for follow-on locking related to a single table.
 *
 * @author Steve Ebersole
 */
public class TableSegment {
	private final TableDetails tableDetails;
	private final EntityMappingType entityMappingType;

	private final QuerySpec querySpec = new QuerySpec( true );

	private final NavigablePath rootPath;
	private final TableReference tableReference;
	private final TableGroup tableGroup;

	private final FollowOnLockingCreationStates creationStates;

	private final List<Integer> statePositions = new ArrayList<>();
	private final List<ResultHandler> resultHandlers = new ArrayList<>();
	private final List<DomainResult<?>> domainResults = new ArrayList<>();

	private final JdbcParameterBindings jdbcParameterBindings;

	public TableSegment(
			TableDetails tableDetails,
			EntityMappingType entityMappingType,
			List<EntityKey> entityKeys,
			SharedSessionContractImplementor session) {
		if ( SQL_EXEC_LOGGER.isDebugEnabled() ) {
			SQL_EXEC_LOGGER.debugf( "Adding table `%s` for follow-on locking - %s", tableDetails.getTableName(), entityMappingType.getEntityName() );
		}

		this.tableDetails = tableDetails;

		this.rootPath = new NavigablePath( tableDetails.getTableName() );
		this.tableReference = new NamedTableReference( tableDetails.getTableName(), "tbl" );
		this.entityMappingType = entityMappingType;
		this.tableGroup = new SimpleTableGroup( tableReference, tableDetails.getTableName(), entityMappingType );

		querySpec.getFromClause().addRoot( tableGroup );

		creationStates = new FollowOnLockingCreationStates(
				querySpec,
				tableGroup,
				entityMappingType.getEntityPersister().getFactory()
		);

		// add the key as the first result
		domainResults.add( tableDetails.getKeyDetails().createDomainResult(
				rootPath.append( "{key}" ),
				tableReference,
				null,
				creationStates
		) );

		final int expectedParamCount = entityKeys.size() * entityMappingType.getIdentifierMapping().getJdbcTypeCount();
		jdbcParameterBindings = new JdbcParameterBindingsImpl( expectedParamCount );

		applyKeyRestrictions( entityKeys, session );
	}

	public void applyAttribute(int index, AttributeMapping attributeMapping) {
		final NavigablePath attributePath = rootPath.append( attributeMapping.getPartName() );
		final DomainResult<Object> domainResult;
		final ResultHandler resultHandler;
		if ( attributeMapping instanceof ToOneAttributeMapping toOne ) {
			domainResult = toOne.getForeignKeyDescriptor().getKeyPart().createDomainResult(
					attributePath,
					tableGroup,
					ForeignKeyDescriptor.PART_NAME,
					creationStates
			);
			resultHandler = new ToOneResultHandler( index, toOne );
		}
		else {
			domainResult = attributeMapping.createDomainResult(
					attributePath,
					tableGroup,
					null,
					creationStates
			);
			resultHandler = new NonToOneResultHandler( index );
		}
		domainResults.add( domainResult );
		statePositions.add( index );
		resultHandlers.add( resultHandler );
	}

	public void applyKeyRestrictions(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		if ( entityMappingType.getIdentifierMapping().getJdbcTypeCount() == 1 ) {
			applySimpleKeyRestriction( entityKeys, session );
		}
		else {
			applyCompositeKeyRestriction( entityKeys, session );
		}
	}

	private void applySimpleKeyRestriction(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();

		final TableDetails.KeyColumn keyColumn = tableDetails.getKeyDetails().getKeyColumn( 0 );
		final ColumnReference columnReference = new ColumnReference( tableReference, keyColumn );

		final InListPredicate restriction = new InListPredicate( columnReference );
		querySpec.applyPredicate( restriction );

		entityKeys.forEach( (entityKey) -> identifierMapping.breakDownJdbcValues(
				entityKey.getIdentifierValue(),
				(valueIndex, value, jdbcValueMapping) -> {
					final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl(
							jdbcValueMapping.getJdbcMapping() );
					restriction.addExpression( jdbcParameter );

					jdbcParameterBindings.addBinding(
							jdbcParameter,
							new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), value )
					);
				},
				session
		) );
	}

	private void applyCompositeKeyRestriction(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();

		final ArrayList<ColumnReference> columnRefs = arrayList( tableDetails.getKeyDetails().getColumnCount() );
		tableDetails.getKeyDetails().forEachKeyColumn( (position, keyColumn) -> {
			columnRefs.add( new ColumnReference( tableReference, keyColumn ) );
		} );
		final SqlTuple keyRef = new SqlTuple( columnRefs, identifierMapping );

		final InListPredicate restriction = new InListPredicate( keyRef );
		querySpec.applyPredicate( restriction );

		entityKeys.forEach( (entityKey) -> {
			final List<JdbcParameterImpl> valueParams = arrayList( tableDetails.getKeyDetails().getColumnCount() );
			identifierMapping.breakDownJdbcValues(
					entityKey.getIdentifierValue(),
					(valueIndex, value, jdbcValueMapping) -> {
						final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcValueMapping.getJdbcMapping() );
						valueParams.add( jdbcParameter );
						jdbcParameterBindings.addBinding(
								jdbcParameter,
								new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), value )
						);
					},
					session
			);
			final SqlTuple valueTuple = new SqlTuple( valueParams, identifierMapping );
			restriction.addExpression( valueTuple );
		} );
	}

	public void performActions(Map<Object, EntityDetails> entityDetailsMap, ExecutionContext lockingExecutionContext) {
		final SharedSessionContractImplementor session = lockingExecutionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

		final SelectStatement selectStatement = new SelectStatement( querySpec, domainResults );
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcServices.getDialect().getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, selectStatement );
		final JdbcOperationQuerySelect jdbcOperation = translator.translate( jdbcParameterBindings, lockingExecutionContext.getQueryOptions() );

		final JdbcSelectExecutor jdbcSelectExecutor = jdbcServices.getJdbcSelectExecutor();
		final List<Object[]> results = jdbcSelectExecutor.executeQuery(
				jdbcOperation,
				jdbcParameterBindings,
				lockingExecutionContext,
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
			final EntityDetails entityDetails = entityDetailsMap.get( id );
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
			entityDetails.entry().getLoadedState()[statePosition] = stateValue;
			entityDetails.key().getPersister().getAttributeMapping( statePosition ).setValue( entityDetails.instance(), stateValue );
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
			entityDetails.entry().getLoadedState()[statePosition] = reference;
			entityDetails.key().getPersister().getAttributeMapping( statePosition ).setValue( entityDetails.instance(), reference );
		}
	}
}
