/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.Iterator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.exec.SqlExecLogger;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class OneToManyRowsInsertExecutor extends OneToManyCreationExecutor {
	private static final Logger log = Logger.getLogger( OneToManyRowsInsertExecutor.class );

	public OneToManyRowsInsertExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			SessionFactoryImplementor sessionFactory) {
		super( collectionDescriptor, dmlTargetTable, sessionFactory );
	}

//	@Override
//	protected JdbcMutation generateCreationOperation(
//			TableReference dmlTableRef,
//			SessionFactoryImplementor sessionFactory,
//			BiConsumer<Column, JdbcParameter> columnConsumer) {
//		// todo (6.0) - add support for writing the index somehow.
//		return super.generateCreationOperation( dmlTableRef, sessionFactory, columnConsumer );
//	}

	@Override
	public void execute(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		if ( key == null ) {
			key = collection.getKey();
		}

		assert key != null;

		final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
		final BasicExecutionContext executionContext = new BasicExecutionContext( session );

		final Iterator entries = collection.entries( getCollectionDescriptor() );

		if ( ! entries.hasNext() ) {
			SqlExecLogger.INSTANCE.debugf(
					"Collection was empty - nothing to (re)create : %s",
					LoggingHelper.toLoggableString( getCollectionDescriptor().getNavigableRole(), collection.getKey() )
			);

			// EARLY EXIT!!
			return;
		}

		int passes = 0;
		int count = 0;
		collection.preInsert( getCollectionDescriptor() );

		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.needsInserting( entry, passes ) ) {
				bindCollectionKey( key, jdbcParameterBindings, session );
				bindCollectionId( entry, passes, collection, jdbcParameterBindings, session );
				bindCollectionIndex( entry, passes, collection, jdbcParameterBindings, session );
				bindCollectionElement( entry, collection, jdbcParameterBindings, session );

				count++;
				JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute(
						getCreationOperation(),
						jdbcParameterBindings,
						executionContext
				);
			}
			passes++;
			jdbcParameterBindings.clear();
		}

		log.debugf( "Done inserting rows: %s inserted", count );
	}
}
