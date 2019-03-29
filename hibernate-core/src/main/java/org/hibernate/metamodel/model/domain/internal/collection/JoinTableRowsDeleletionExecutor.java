/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.LoadParameterBindingContext;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;

import org.jboss.logging.Logger;

/**
 * @author Andrea Boriero
 */
public class JoinTableRowsDeleletionExecutor implements CollectionRowsDeletionExecutor {
	private static final Logger log = Logger.getLogger( JoinTableRowsDeleletionExecutor.class );

	private final PersistentCollectionDescriptor collectionDescriptor;
	private boolean deleteByIndex;
	private Map<Column, JdbcParameter> jdbcParameterMap;
	private final JdbcDelete removalOperation;

	public JoinTableRowsDeleletionExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory,
			boolean deleteByIndex) {
		this.collectionDescriptor = collectionDescriptor;
		this.deleteByIndex = deleteByIndex;
		final TableReference collectionTableRef = new TableReference(
				collectionDescriptor.getSeparateCollectionTable(),
				null,
				false
		);

		Map<Column, JdbcParameter> jdbcParameterMap = new HashMap<>();
		final DeleteStatement deleteStatement = generateDeleteStatement(
				collectionTableRef,
				jdbcParameterMap::put,
				sessionFactory
		);
		this.jdbcParameterMap = jdbcParameterMap;
		removalOperation = SqlDeleteToJdbcDeleteConverter.interpret(
				deleteStatement,
				sessionFactory
		);
	}

	private DeleteStatement generateDeleteStatement(
			TableReference collectionTableRef,
			BiConsumer<Column, JdbcParameter> parameterCollector,
			SessionFactoryImplementor sessionFactory) {

		final AtomicInteger parameterCount = new AtomicInteger();

		final Junction deleteRestriction = new Junction( Junction.Nature.CONJUNCTION );

		if ( collectionDescriptor.getIdDescriptor() != null ) {
			collectionDescriptor.getIdDescriptor().visitColumns(
					(BiConsumer<SqlExpressableType, Column>) (jdbcType, column) -> {
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.DELETE,
								sessionFactory.getTypeConfiguration()
						);

						parameterCollector.accept( column, parameter );

						deleteRestriction.add(
								new ComparisonPredicate(
										collectionTableRef.qualify( column ), ComparisonOperator.EQUAL,
										parameter
								)
						);
					},
					Clause.DELETE,
					sessionFactory.getTypeConfiguration()
			);
		}
		else {
			collectionDescriptor.getCollectionKeyDescriptor().visitColumns(
					(BiConsumer<SqlExpressableType, Column>) (jdbcType, column) -> {
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.DELETE,
								sessionFactory.getTypeConfiguration()
						);

						parameterCollector.accept( column, parameter );

						deleteRestriction.add(
								new ComparisonPredicate(
										collectionTableRef.qualify( column ), ComparisonOperator.EQUAL,
										parameter
								)
						);
					},
					Clause.DELETE,
					sessionFactory.getTypeConfiguration()
			);
			if ( deleteByIndex ) {
				collectionDescriptor.getIndexDescriptor().visitColumns(
						(BiConsumer<SqlExpressableType, Column>) (jdbcType, column) -> {
							final PositionalParameter parameter = new PositionalParameter(
									parameterCount.getAndIncrement(),
									column.getExpressableType(),
									Clause.DELETE,
									sessionFactory.getTypeConfiguration()
							);

							parameterCollector.accept( column, parameter );

							deleteRestriction.add(
									new ComparisonPredicate(
											collectionTableRef.qualify( column ), ComparisonOperator.EQUAL,
											parameter
									)
							);
						},
						Clause.DELETE,
						sessionFactory.getTypeConfiguration()
				);
			}
			else {
				collectionDescriptor.getElementDescriptor().visitColumns(
						(BiConsumer<SqlExpressableType, Column>) (jdbcType, column) -> {
							final PositionalParameter parameter = new PositionalParameter(
									parameterCount.getAndIncrement(),
									column.getExpressableType(),
									Clause.DELETE,
									sessionFactory.getTypeConfiguration()
							);

							parameterCollector.accept( column, parameter );

							deleteRestriction.add(
									new ComparisonPredicate(
											collectionTableRef.qualify( column ), ComparisonOperator.EQUAL,
											parameter
									)
							);
						},
						Clause.DELETE,
						sessionFactory.getTypeConfiguration()
				);
			}
		}

		return new DeleteStatement( collectionTableRef, deleteRestriction );
	}


	@Override
	public void execute(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		Iterator deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Deleting rows of collection: %s",
					MessageHelper.collectionInfoString( collectionDescriptor, collection, key, session )
			);
		}
		if ( deletes.hasNext() ) {
			int passes = 0;
			final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
			final BasicExecutionContext executionContext = new BasicExecutionContext(
					session,
					new LoadParameterBindingContext( session.getSessionFactory(), key )
			);

			while ( deletes.hasNext() ) {
				Object entry = deletes.next();
				if ( collectionDescriptor.getIdDescriptor() != null ) {
					bindCollectionId( entry, passes, collection, jdbcParameterBindings, session );
				}
				else {
					bindCollectionKey( key, jdbcParameterBindings, session );
					if ( deleteByIndex ) {
						bindCollectionIndex( entry, jdbcParameterBindings, session );
					}
					else {
						bindCollectionElement( entry, collection, jdbcParameterBindings, session );
					}
				}
				JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute(
						removalOperation,
						jdbcParameterBindings,
						executionContext
				);

				passes++;
				jdbcParameterBindings.clear();
				log.debugf( "Done deleting collection rows: %s deleted", passes );
			}
		}
		else {
			log.debug( "No rows to delete" );
		}
	}

	protected void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		collectionDescriptor.getElementDescriptor().dehydrate(
				collectionDescriptor.getElementDescriptor().unresolve(
						collection.getElement( entry, collectionDescriptor ),
						session
				),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.DELETE,
				session
		);
	}

	protected void bindCollectionIndex(
			Object index,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final CollectionIndex indexDescriptor = collectionDescriptor.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			if ( indexDescriptor.getBaseIndex() != 0 ) {
				index = (Integer) index + indexDescriptor.getBaseIndex();
			}
			indexDescriptor.dehydrate(
					indexDescriptor.unresolve( index, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session
					),
					Clause.DELETE,
					session
			);
		}
	}

	protected void bindCollectionKey(
			Object key,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
				collectionDescriptor.getCollectionKeyDescriptor().unresolve( key, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.DELETE,
				session
		);
	}

	protected void bindCollectionId(
			Object entry,
			int assumedIdentifier,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		// todo (6.0) : probably not the correct `assumedIdentifier`
		final Object identifier = collection.getIdentifier( entry, assumedIdentifier, collectionDescriptor );

		collectionDescriptor.getIdDescriptor().dehydrate(
				collectionDescriptor.getIdDescriptor().unresolve( identifier, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.DELETE,
				session
		);
	}

	protected void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType type,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						type,
						Clause.DELETE,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	private JdbcParameter resolveJdbcParameter(Column boundColumn) {
		final JdbcParameter jdbcParameter = jdbcParameterMap.get( boundColumn );
		if ( jdbcParameter == null ) {
			throw new IllegalStateException( "JdbcParameter not found for Column [" + boundColumn + "]" );
		}
		return jdbcParameter;
	}
}
