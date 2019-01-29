/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.tree.spi.DeleteStatement;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public abstract class AbstractCollectionRowsDeletionExecutor implements CollectionRowsDeletionExecutor {
	private static final Logger log = Logger.getLogger( AbstractCollectionRowsDeletionExecutor.class );

	private final PersistentCollectionDescriptor collectionDescriptor;
	private final boolean deleteByIndex;
	private final Map<Column, JdbcParameter> jdbcParameterMap;
	private final JdbcDelete deleteOperation;

	public AbstractCollectionRowsDeletionExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory,
			Table collectionTable,
			boolean deleteByIndex) {
		this.collectionDescriptor = collectionDescriptor;
		this.deleteByIndex = deleteByIndex;

		final TableReference collectionTableRef = new TableReference(
				collectionTable,
				null,
				false
		);

		Map<Column, JdbcParameter> jdbcParameterMap = new HashMap<>();

		this.deleteOperation = SqlDeleteToJdbcDeleteConverter.interpret(
				generateRowsDeletionOperation(
						collectionTableRef,
						sessionFactory,
						jdbcParameterMap::put
				),
				sessionFactory
		);

		this.jdbcParameterMap = jdbcParameterMap;
	}

	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}

	public boolean isDeleteByIndex() {
		return deleteByIndex;
	}

	@Override
	public void execute(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		Iterator deletes = collection.getDeletes( getCollectionDescriptor(), !isDeleteByIndex() );
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Deleting rows of collection: %s",
					MessageHelper.collectionInfoString( getCollectionDescriptor(), collection, key, session )
			);
		}
		if ( deletes.hasNext() ) {
			int passes = 0;
			final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
			final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

			while ( deletes.hasNext() ) {
				Object entry = deletes.next();
				if ( getCollectionDescriptor().getIdDescriptor() != null ) {
					bindCollectionId( entry, passes, collection, jdbcParameterBindings, session, Clause.DELETE );
				}
				else {
					bindCollectionKey( key, jdbcParameterBindings, session, Clause.DELETE );
					if ( isDeleteByIndex() ) {
						bindCollectionIndex( entry, passes, collection, jdbcParameterBindings, session, Clause.DELETE );
					}
					else {
						bindCollectionElement( entry, collection, jdbcParameterBindings, session, Clause.DELETE );
					}
				}
				JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( deleteOperation, executionContext );

				passes++;
				jdbcParameterBindings.clear();
				log.debugf( "Done deleting collection rows: %s deleted", passes );
			}
		}
		else {
			log.debug( "No rows to delete" );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected abstract DeleteStatement generateRowsDeletionOperation(
			TableReference collectioNTableRef,
			SessionFactoryImplementor sessionFactory,
			BiConsumer<Column, JdbcParameter> columnConsumer);

	protected void applyNavigablePredicate(
			Navigable<?> navigable,
			TableReference collectionTableRef,
			AtomicInteger parameterCount,
			BiConsumer<Column, JdbcParameter> parameterCollector,
			Consumer<Predicate> predicateConsumer,
			SessionFactoryImplementor sessionFactory) {
		navigable.visitColumns(
				(jdbcType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.DELETE,
							sessionFactory.getTypeConfiguration()
					);

					parameterCollector.accept( column, parameter );

					predicateConsumer.accept(
							new ComparisonPredicate(
									collectionTableRef.qualify( column ),
									ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.DELETE,
				sessionFactory.getTypeConfiguration()
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType sqlExpressableType,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session,
			Clause clause) {
		final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						sqlExpressableType,
						clause,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionId(
			Object entry,
			int assumedIdentifier,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session,
			Clause clause) {
		// todo (6.0) - probably not the correct `assumedIdentifier`
		final Object identifier = collection.getIdentifier( entry, assumedIdentifier, collectionDescriptor );

		collectionDescriptor.getIdDescriptor().dehydrate(
				collectionDescriptor.getIdDescriptor().unresolve( identifier, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session,
						clause
				),
				clause,
				session
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionKey(
			Object key,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session,
			Clause clause) {
		collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
				collectionDescriptor.getCollectionKeyDescriptor().unresolve( key, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session,
						clause
				),
				clause,
				session
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionIndex(
			Object entry,
			int assumedIndex,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session,
			Clause clause) {
		// todo (6.0) : probably not the correct `assumedIndex`
		if ( collectionDescriptor.getIndexDescriptor() != null ) {
			final Object index = collection.getIndex( entry, assumedIndex, collectionDescriptor );
			collectionDescriptor.getIndexDescriptor().dehydrate(
					collectionDescriptor.getIndexDescriptor().unresolve( index, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session,
							clause
					),
					clause,
					session
			);
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session,
			Clause clause) {
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
						session,
						clause
				),
				clause,
				session
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
