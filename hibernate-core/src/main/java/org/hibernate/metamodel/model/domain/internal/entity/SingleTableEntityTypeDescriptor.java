/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.loader.internal.TemplateParameterBindingContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.InsertToJdbcInsertConverter;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstDeleteDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.DeleteStatement;
import org.hibernate.sql.ast.tree.spi.InsertStatement;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcUpdate;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SingleTableEntityTypeDescriptor<T> extends AbstractEntityTypeDescriptor<T> {
	private Boolean hasCollections;
	private final boolean isJpaCacheComplianceEnabled;

	public SingleTableEntityTypeDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor<? super T> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
		isJpaCacheComplianceEnabled = creationContext.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaCacheComplianceEnabled();
	}


	// `select ... from Person p order by p`
	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return sourceSqmFrom.getNavigableReference();
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext resolutionContext) {
		return getIdentifierDescriptor().resolveColumnReferences( qualifier, resolutionContext );
	}

	@Override
	public String asLoggableText() {
		return String.format( "SingleTableEntityDescriptor<%s>", getEntityName() );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return Collections.emptySet();
	}

	@Override
	public int[] findDirty(
			Object[] currentState,
			Object[] previousState,
			Object owner,
			SharedSessionContractImplementor session) {
		final List<Integer> results = new ArrayList<>();

		visitStateArrayContributors(
				contributor -> {
					final int index = contributor.getStateArrayPosition();
					final boolean dirty = currentState[index] != LazyPropertyInitializer.UNFETCHED_PROPERTY &&
							( previousState[index] == LazyPropertyInitializer.UNFETCHED_PROPERTY ||
									( contributor.isIncludedInDirtyChecking() &&
											contributor.isDirty( previousState[index], currentState[index], session ) ) );

					if ( dirty ) {
						results.add( index );
					}
				}
		);

		if ( results.size() == 0 ) {
			return null;
		}
		else {
			return results.stream().mapToInt( i-> i ).toArray();
		}
	}

	@Override
	public int[] findModified(
			Object[] old, Object[] current, Object object, SharedSessionContractImplementor session) {
		return new int[0];
	}

	@Override
	public void lock(
			Object id, Object version, Object object, LockMode lockMode, SharedSessionContractImplementor session)
			throws HibernateException {

	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) throws HibernateException {

	}

	protected Object insertInternal(
			Object id,
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		// generate id if needed
		if ( id == null ) {
			final IdentifierGenerator generator = getHierarchy().getIdentifierDescriptor().getIdentifierValueGenerator();
			if ( generator != null ) {
				id = generator.generate( session, object );
			}
		}

//		final Object unresolvedId = getHierarchy().getIdentifierDescriptor().unresolve( id, session );
		final Object unresolvedId = id;
		final ExecutionContext executionContext = getExecutionContext( session );

		// for now - just root table
		// for now - we also regenerate these SQL AST objects each time - we can cache these
		executeInsert( fields, session, unresolvedId, executionContext, new TableReference( getPrimaryTable(), null, false) );

		getSecondaryTableBindings().forEach(
				tableBindings -> executeJoinTableInsert(
						fields,
						session,
						unresolvedId,
						executionContext,
						tableBindings
				)
		);

		return id;
	}

	private void executeJoinTableInsert(
			Object[] fields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			JoinedTableBinding tableBindings) {
		if ( tableBindings.isInverse() ) {
			return;
		}

		final TableReference tableReference = new TableReference( tableBindings.getReferringTable(), null , tableBindings.isOptional());
		final ValuesNullChecker jdbcValuesToInsert = new ValuesNullChecker();
		final InsertStatement insertStatement = new InsertStatement( tableReference );

		visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					final Object domainValue = fields[position];
					contributor.dehydrate(
							// todo (6.0) : fix this - specifically this isInstance check is bad
							// 		sometimes the values here are unresolved and sometimes not;
							//		need a way to ensure they are always one form or the other
							//		during these calls (ideally unresolved)
							contributor.getJavaTypeDescriptor().isInstance( domainValue )
									? contributor.unresolve( domainValue, session )
									: domainValue,
							(jdbcValue, type, boundColumn) -> {
								if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
									if ( jdbcValue != null ) {
										jdbcValuesToInsert.setNotAllNull();
										addInsertColumn( session, insertStatement, jdbcValue, boundColumn, type );
									}
								}
							},
							Clause.INSERT,
							session
					);
				}
		);

		if ( jdbcValuesToInsert.areAllNull() ) {
			return;
		}

		getHierarchy().getIdentifierDescriptor().dehydrate(
				// NOTE : at least according to the argument name (`unresolvedId`), the
				// 		incoming id value should already be unresolved - so do not
				// 		unresolve it again
				getHierarchy().getIdentifierDescriptor().unresolve( unresolvedId, session ),
				//unresolvedId,
				(jdbcValue, type, boundColumn) -> {
					final Column referringColumn = tableBindings.getJoinForeignKey()
							.getColumnMappings()
							.findReferringColumn( boundColumn );
					addInsertColumn(
							session,
							insertStatement,
							jdbcValue,
							referringColumn,
							boundColumn.getExpressableType()
					);
				},
				Clause.INSERT,
				session
		);

		final TenantDiscrimination tenantDiscrimination = getHierarchy().getTenantDiscrimination();
		if ( tenantDiscrimination != null ) {
			addInsertColumn(
					session,
					insertStatement,
					tenantDiscrimination.unresolve( session.getTenantIdentifier(), session ),
					tenantDiscrimination.getBoundColumn(),
					tenantDiscrimination.getBoundColumn().getExpressableType()
			);
		}

		executeInsert( executionContext, insertStatement );
	}

	private void executeInsert(
			Object[] fields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			TableReference tableReference) {

		final InsertStatement insertStatement = new InsertStatement( tableReference );
		// todo (6.0) : account for non-generated identifiers

		getHierarchy().getIdentifierDescriptor().dehydrate(
				// NOTE : at least according to the argument name (`unresolvedId`), the
				// 		incoming id value should already be unresolved - so do not
				// 		unresolve it again
				getHierarchy().getIdentifierDescriptor().unresolve( unresolvedId, session ),
				//unresolvedId,
				(jdbcValue, type, boundColumn) -> {
					insertStatement.addTargetColumnReference( new ColumnReference( boundColumn ) );
					insertStatement.addValue(
							new LiteralParameter(
									jdbcValue,
									boundColumn.getExpressableType(),
									Clause.INSERT,
									session.getFactory().getTypeConfiguration()
							)
					);
				},
				Clause.INSERT,
				session
		);

		final DiscriminatorDescriptor<Object> discriminatorDescriptor = getHierarchy().getDiscriminatorDescriptor();
		if ( discriminatorDescriptor != null ) {
			addInsertColumn(
					session,
					insertStatement,
					discriminatorDescriptor.unresolve( getDiscriminatorValue(), session ),
					discriminatorDescriptor.getBoundColumn(),
					discriminatorDescriptor.getBoundColumn().getExpressableType()
			);
		}

		final TenantDiscrimination tenantDiscrimination = getHierarchy().getTenantDiscrimination();
		if ( tenantDiscrimination != null ) {
			addInsertColumn(
					session,
					insertStatement,
					tenantDiscrimination.unresolve( session.getTenantIdentifier(), session ),
					tenantDiscrimination.getBoundColumn(),
					tenantDiscrimination.getBoundColumn().getExpressableType()
			);
		}

		visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					final Object domainValue = fields[position];
					contributor.dehydrate(
							// todo (6.0) : fix this - specifically this isInstance check is bad
							// 		sometimes the values here are unresolved and sometimes not;
							//		need a way to ensure they are always one form or the other
							//		during these calls (ideally unresolved)
							contributor.getJavaTypeDescriptor().isInstance( domainValue )
									? contributor.unresolve( domainValue, session )
									: domainValue,
							(jdbcValue, type, boundColumn) -> {
								if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
									addInsertColumn( session, insertStatement, jdbcValue, boundColumn, type );
								}
							},
							Clause.INSERT,
							session
					);
				}
		);

		executeInsert( executionContext, insertStatement );
	}

	private void executeInsert(ExecutionContext executionContext, InsertStatement insertStatement) {
		JdbcMutation jdbcInsert = InsertToJdbcInsertConverter.createJdbcInsert(
				insertStatement,
				executionContext.getSession().getSessionFactory()
		);
		executeOperation( executionContext, jdbcInsert, (rows, prepareStatement) -> {} );
	}

	private void addInsertColumn(
			SharedSessionContractImplementor session,
			InsertStatement insertStatement,
			Object jdbcValue,
			Column referringColumn,
			SqlExpressableType expressableType) {
		if ( jdbcValue != null ) {
			insertStatement.addTargetColumnReference( new ColumnReference( referringColumn ) );
			insertStatement.addValue(
					new LiteralParameter(
							jdbcValue,
							expressableType,
							Clause.INSERT,
							session.getFactory().getTypeConfiguration()
					)
			);
		}
	}

	@Override
	public void delete(
			Object id,
			Object version,
			Object object,
			SharedSessionContractImplementor session)
			throws HibernateException {

		// todo (6.0) - initial basic pass at entity deletes

		final Object unresolvedId = getHierarchy().getIdentifierDescriptor().unresolve( id, session );
		final ExecutionContext executionContext = getExecutionContext( session );


		deleteSecondaryTables( session, unresolvedId, executionContext );

		deleteRootTable( session, unresolvedId, executionContext );
	}

	private void deleteRootTable(
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext) {
		final TableReference tableReference = new TableReference( getPrimaryTable(), null, false );

		final Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
		getHierarchy().getIdentifierDescriptor().dehydrate(
				unresolvedId,
				(jdbcValue, type, boundColumn) ->
						identifierJunction.add(
								new RelationalPredicate(
										RelationalPredicate.Operator.EQUAL,
										new ColumnReference( boundColumn ),
										new LiteralParameter(
												jdbcValue,
												boundColumn.getExpressableType(),
												Clause.DELETE,
												session.getFactory().getTypeConfiguration()
										)
								)
						)
				,
				Clause.DELETE,
				session
		);

		executeDelete( executionContext, tableReference, identifierJunction );
	}

	private void deleteSecondaryTables(
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext) {
		getSecondaryTableBindings().forEach( secondaryTable -> {
			final TableReference secondaryTableReference = new TableReference(
					secondaryTable.getReferringTable(),
					null,
					secondaryTable.isOptional()
			);
			final Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
			getHierarchy().getIdentifierDescriptor().dehydrate(
					unresolvedId,
					(jdbcValue, type, boundColumn) -> {
						final Column referringColumn = secondaryTable.getJoinForeignKey()
								.getColumnMappings()
								.findReferringColumn( boundColumn );
						identifierJunction.add(
								new RelationalPredicate(
										RelationalPredicate.Operator.EQUAL,
										new ColumnReference( referringColumn ),
										new LiteralParameter(
												jdbcValue,
												boundColumn.getExpressableType(),
												Clause.DELETE,
												session.getFactory().getTypeConfiguration()
										)
								)
						);
					},
					Clause.DELETE,
					session
			);

			executeDelete( executionContext, secondaryTableReference, identifierJunction );
		} );
	}

	private void executeDelete(
			ExecutionContext executionContext,
			TableReference tableReference,
			Junction identifierJunction) {
		final DeleteStatement deleteStatement = new DeleteStatement( tableReference, identifierJunction );

		final JdbcMutation delete = SqlDeleteToJdbcDeleteConverter.interpret(
				new SqlAstDeleteDescriptor() {
					@Override
					public DeleteStatement getSqlAstStatement() {
						return deleteStatement;
					}

					@Override
					public Set<String> getAffectedTableNames() {
						return Collections.singleton(
								deleteStatement.getTargetTable().getTable().getTableExpression()
						);
					}
				},
				executionContext.getSession().getSessionFactory()
		);

		executeOperation( executionContext, delete , (rows, prepareStatement) -> {} );
	}

	@Override
	public void update(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) throws HibernateException {

		// todo (6.0) - initial basic pass at entity updates
		// todo (6.0) - apply any pre-update in-memory value generation
		EntityEntry entry = session.getPersistenceContext().getEntry( object );

		if ( entry == null && !getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet!" );
		}
		final Object unresolvedId = getHierarchy().getIdentifierDescriptor().unresolve( id, session );
		final ExecutionContext executionContext = getExecutionContext( session );

		Table primaryTable = getPrimaryTable();

		final TableReference tableReference = new TableReference( primaryTable, null, false );
		if ( isTableNeedUpdate( tableReference, dirtyFields, hasDirtyCollection, true ) ) {
			updateInternal(
					fields,
					dirtyFields,
					oldFields,
					session,
					unresolvedId,
					executionContext,
					tableReference,
					Expectations.appropriateExpectation( rootUpdateResultCheckStyle )
			);
		}

		getSecondaryTableBindings().forEach(
				secondaryTable -> {
					final TableReference secondaryTableReference = new TableReference(
							secondaryTable.getReferringTable(),
							null,
							secondaryTable.isOptional()
					);
					if (
							!secondaryTable.isInverse()
									&& isTableNeedUpdate(
									secondaryTableReference,
									dirtyFields,
									hasDirtyCollection,
									false
							)
					) {
						final boolean isRowToInsert = updateInternal(
								fields,
								dirtyFields,
								oldFields,
								session,
								unresolvedId,
								executionContext,
								secondaryTableReference,
								Expectations.appropriateExpectation( secondaryTable.getUpdateResultCheckStyle() )
						);

						if ( isRowToInsert ) {
							executeJoinTableInsert(
									fields,
									session,
									unresolvedId,
									executionContext,
									secondaryTable
							);
						}
					}
				}
		);
	}

	/**
	 *
	 * @return true if an insert operation is required
	 */
	private boolean updateInternal(
			Object[] fields,
			int[] dirtyFields,
			Object[] oldFields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			TableReference tableReference,
			Expectation expectation) {
		final boolean isRowToUpdate;
		final boolean isNullableTable = isNullableTable( tableReference );
		final boolean isFieldsAllNull = isAllNull( fields, tableReference.getTable() );
		if ( isNullableTable && oldFields != null && isAllNull( oldFields, tableReference.getTable() ) ) {
			isRowToUpdate = false;
		}
		else if ( isNullableTable && isFieldsAllNull ) {
			//if all fields are null, we might need to delete existing row
			isRowToUpdate = true;
			// TODO (6.0) : delete the existing row
		}
		else {
			//there is probably a row there, so try to update
			//if no rows were updated, we will find out
			// TODO (6.0) : update should return a boolean value to be assigned to isRowToUpdate
			RowToUpdateChecker checker = new RowToUpdateChecker(
					unresolvedId,
					isNullableTable,
					expectation,
					getFactory(),
					this
			);
			executeUpdate(
					fields,
					dirtyFields,
					session,
					unresolvedId,
					executionContext,
					tableReference,
					checker
			);
			isRowToUpdate = checker.isRowToUpdate();
		}
		return !isRowToUpdate && !isFieldsAllNull;
	}

	private int executeUpdate(
			Object[] fields,
			int[] dirtyFields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			TableReference tableReference,
			RowToUpdateChecker checker) {
		List<Assignment> assignments = new ArrayList<>();
		for ( int dirtyField : dirtyFields ) {
			final StateArrayContributor contributor = getStateArrayContributors().get( dirtyField );
			final Object domainValue = fields[contributor.getStateArrayPosition()];
			List<Column> columns = contributor.getColumns();
			if ( columns != null && !columns.isEmpty() ) {
				if ( contributor.isUpdatable() ) {
					contributor.dehydrate(
							contributor.unresolve( domainValue, session ),
							(jdbcValue, type, boundColumn) -> {
								if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
									assignments.add(
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
			}
		}

		Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
		getHierarchy().getIdentifierDescriptor().dehydrate(
				unresolvedId,
				(jdbcValue, type, boundColumn) ->
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
						)
				,
				Clause.WHERE,
				session
		);

		// todo (6.0) : depending on optimistic-lock strategy may need to adjust where clause

		final UpdateStatement updateStatement = new UpdateStatement(
				tableReference,
				assignments,
				identifierJunction
		);


		return executeUpdate( executionContext, updateStatement, checker  );
	}

	private int executeUpdate(ExecutionContext executionContext, UpdateStatement updateStatement,  RowToUpdateChecker checker) {
		JdbcUpdate jdbcUpdate = UpdateToJdbcUpdateConverter.createJdbcUpdate(
				updateStatement,
				executionContext.getSession().getSessionFactory()
		);
		return executeOperation(
				executionContext,
				jdbcUpdate,
				(rows, prepareStatement) -> checker.check( rows, prepareStatement )
		);
	}

	private int executeOperation(
			ExecutionContext executionContext,
			JdbcMutation operation,
			BiConsumer<Integer, PreparedStatement> checker) {
		final JdbcMutationExecutor executor = JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL;
		return executor.execute(
				operation,
				executionContext,
				Connection::prepareStatement,
				(rows, preparestatement) -> checker.accept( rows, preparestatement )
		);
	}

	protected final boolean isAllNull(Object[] fields, Table table) {
		final List<StateArrayContributor<?>> stateArrayContributors = getStateArrayContributors();
		for ( int i = 0; i < fields.length; i++ ) {
			if ( fields[i] != null ) {
				final List<Column> columns = stateArrayContributors.get( i ).getColumns();
				for ( Column column : columns ) {
					if ( column.getSourceTable().equals( table ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean isTableNeedUpdate(
			TableReference tableReference,
			int[] dirtyProperties,
			boolean hasDirtyCollection,
			boolean isRootTable) {
		if ( dirtyProperties == null ) {
			// TODO (6.0) : isTableNeedUpdate() to implement case dirtyProperties == null
			throw new NotYetImplementedFor6Exception( getClass() );
		}
		else {
			boolean tableNeedUpdate = false;
			final List<StateArrayContributor<?>> stateArrayContributors = getStateArrayContributors();
			for ( int property : dirtyProperties ) {
				final StateArrayContributor<?> contributor = stateArrayContributors.get( property );
				final List<Column> columns = contributor.getColumns();
				for ( Column column : columns ) {
					if ( column.getSourceTable().equals( tableReference.getTable() )
							&& contributor.isUpdatable() ) {
						tableNeedUpdate = true;
					}
				}
			}
			if ( isRootTable && getHierarchy().getVersionDescriptor() != null ) {
				tableNeedUpdate = tableNeedUpdate ||
						Versioning.isVersionIncrementRequired(
								dirtyProperties,
								hasDirtyCollection,
								getPropertyVersionability()
						);
			}
			return tableNeedUpdate;
		}
	}

	private boolean isNullableTable(TableReference tableReference) {
		return tableReference.isOptional() || isJpaCacheComplianceEnabled;
	}

	private ExecutionContext getExecutionContext(SharedSessionContractImplementor session) {
		return new ExecutionContext() {
			private final ParameterBindingContext parameterBindingContext = new TemplateParameterBindingContext( session.getFactory() );

			@Override
			public SharedSessionContractImplementor getSession() {
				return session;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return new QueryOptionsImpl();
			}

			@Override
			public ParameterBindingContext getParameterBindingContext() {
				return parameterBindingContext;
			}

			@Override
			public Callback getCallback() {
				return afterLoadAction -> {
				};
			}
		};
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[0];
	}

	@Override
	public JavaTypeDescriptor[] getPropertyJavaTypeDescriptors() {
		return null;
	}

	@Override
	public String[] getPropertyNames() {
		return new String[0];
	}

	@Override
	public boolean[] getPropertyInsertability() {
		return new boolean[0];
	}

	@Override
	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		return new ValueInclusion[0];
	}

	@Override
	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		return new ValueInclusion[0];
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		return new boolean[0];
	}

	@Override
	public boolean[] getPropertyCheckability() {
		return new boolean[0];
	}

	@Override
	public boolean[] getPropertyNullability() {
		return new boolean[0];
	}

	@Override
	public boolean[] getPropertyVersionability() {
		return new boolean[0];
	}

	@Override
	public boolean[] getPropertyLaziness() {
		return new boolean[0];
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		return new CascadeStyle[0];
	}

	@Override
	public boolean hasCascades() {
		return false;
	}

	@Override
	public Type getIdentifierType() {
		return null;
	}

	@Override
	public String getIdentifierPropertyName() {
		return null;
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		return false;
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		return false;
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return null;
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public boolean isBatchLoadable() {
		return false;
	}

	@Override
	public boolean isSelectBeforeUpdateRequired() {
		return false;
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public Object forceVersionIncrement(
			Object id, Object currentVersion, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public boolean isInstrumented() {
		return false;
	}

	@Override
	public boolean hasInsertGeneratedProperties() {
		return false;
	}

	@Override
	public boolean hasUpdateGeneratedProperties() {
		return false;
	}

	@Override
	public boolean isVersionPropertyGenerated() {
		return false;
	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {

	}

	@Override
	public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
		final Object id = getHierarchy().getIdentifierDescriptor().extractIdentifier( object, session );

		// we *always* assume an instance with a null
		// identifier or no identifier property is unsaved.
		if ( id == null ) {
			return Boolean.TRUE;
		}

		// check the version unsaved-value, if appropriate
		final Object version = getVersion( object );
		if ( getHierarchy().getVersionDescriptor() != null ) {
			// let this take precedence if defined, since it works for assigned identifiers
			// todo (6.0) - this may require some more work to handle proper comparisons.
			return getHierarchy().getVersionDescriptor().getUnsavedValue() == version;
		}

		// check the id unsaved-value
		// todo (6.0) - need to implement this behavior

		// check to see if it is in the second-level cache
		if ( session.getCacheMode().isGetEnabled() && canReadFromCache() ) {
			// todo (6.0) - support reading from the cache
		}

		return null;
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object object,
			Map mergeMap,
			SharedSessionContractImplementor session) throws HibernateException {
		final Object[] stateArray = new Object[ getStateArrayContributors().size() ];
		visitStateArrayContributors(
				contributor -> {
					stateArray[ contributor.getStateArrayPosition() ] = contributor.getPropertyAccess().getGetter().getForInsert(
							object,
							mergeMap,
							session
					);
				}
		);

		return stateArray;
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {

	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {

	}

	@Override
	public Class getMappedClass() {
		return null;
	}

	@Override
	public boolean implementsLifecycle() {
		return false;
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		return false;
	}

	@Override
	public EntityTypeDescriptor getSubclassEntityPersister(
			Object instance, SessionFactoryImplementor factory) {
		if ( getSubclassTypes().isEmpty() ) {
			return this;
		}
		else {
			throw new NotYetImplementedFor6Exception(  );
		}
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return null;
	}

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		return new int[0];
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		return false;
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {

	}

	@Override
	public boolean hasCollections() {
		// todo (6.0) : do this init up front?
		if ( hasCollections == null ) {
			hasCollections = false;
			controlledVisitAttributes(
					attr -> {
						if ( attr instanceof PluralPersistentAttribute ) {
							hasCollections = true;
							return false;
						}

						return true;
					}
			);
		}

		return hasCollections;
	}

	private static class RowToUpdateChecker {
		private final Object id;
		private final boolean isNullableTable;
		private final Expectation expectation;
		private final SessionFactoryImplementor factory;
		private final EntityTypeDescriptor entityDescriptor;

		private boolean isRowToUpdate;

		public RowToUpdateChecker(
				Object id,
				boolean isNullableTable,
				Expectation expectation,
				SessionFactoryImplementor factory,
				EntityTypeDescriptor entityDescriptor) {
			this.id = id;
			this.isNullableTable = isNullableTable;
			this.expectation = expectation;
			this.factory = factory;
			this.entityDescriptor = entityDescriptor;
		}

		public void check(Integer rows, PreparedStatement preparedStatement) {
			try {
				expectation.verifyOutcome( rows, preparedStatement, -1 );
			}
			catch (StaleStateException e) {
				if ( isNullableTable ) {
					if ( factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().optimisticFailure( entityDescriptor.getEntityName() );
					}
					throw new StaleObjectStateException( entityDescriptor.getEntityName(), id );
				}
				isRowToUpdate = false;
			}
			catch (TooManyRowsAffectedException e) {
				throw new HibernateException(
						"Duplicate identifier in table for: " +
								MessageHelper.infoString( entityDescriptor, id, factory )
				);
			}
			catch (Throwable t) {
				isRowToUpdate = false;
			}
			isRowToUpdate = true;
		}

		public boolean isRowToUpdate() {
			return isRowToUpdate;
		}
	}

	private class ValuesNullChecker {
		private boolean allNull = true;

		private void setNotAllNull(){
			allNull = false;
		}

		public boolean areAllNull(){
			return allNull;
		}
	}

}
