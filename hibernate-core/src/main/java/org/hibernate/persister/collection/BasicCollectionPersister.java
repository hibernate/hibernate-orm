/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Delete;
import org.hibernate.sql.Insert;
import org.hibernate.sql.Update;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * A {@link CollectionPersister} for {@linkplain jakarta.persistence.ElementCollection
 * collections of values} and {@linkplain jakarta.persistence.ManyToMany many-to-many
 * associations}.
 *
 * @see OneToManyPersister
 *
 * @author Gavin King
 */
public class BasicCollectionPersister extends AbstractCollectionPersister {

	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Deprecated(since = "6.0")
	public BasicCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext creationContext) throws MappingException, CacheException {
		this( collectionBinding, cacheAccessStrategy, (RuntimeModelCreationContext) creationContext );
	}

	public BasicCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
	}

	/**
	 * Generate the SQL DELETE that deletes all rows
	 */
	@Override
	protected String generateDeleteString() {
		final Delete delete = createDelete().setTableName( qualifiedTableName )
				.addPrimaryKeyColumns( keyColumnNames );

		if ( hasWhere ) {
			delete.setWhere( sqlWhereString );
		}

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( "delete collection " + getRole() );
		}

		return delete.toStatementString();
	}

	/**
	 * Generate the SQL INSERT that creates a new row
	 */
	@Override
	protected String generateInsertRowString() {
		final Insert insert = createInsert().setTableName( qualifiedTableName )
				.addColumns( keyColumnNames );

		if ( hasIdentifier ) {
			insert.addColumn( identifierColumnName );
		}

		if ( hasIndex /*&& !indexIsFormula*/ ) {
			insert.addColumns( indexColumnNames, indexColumnIsSettable );
		}

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			insert.setComment( "insert collection row " + getRole() );
		}

		//if ( !elementIsFormula ) {
		insert.addColumns( elementColumnNames, elementColumnIsSettable, elementColumnWriters );
		//}

		return insert.toStatementString();
	}

	/**
	 * Generate the SQL UPDATE that updates a row
	 */
	@Override
	protected String generateUpdateRowString() {
		final Update update = createUpdate().setTableName( qualifiedTableName );

		//if ( !elementIsFormula ) {
		update.addColumns( elementColumnNames, elementColumnIsSettable, elementColumnWriters );
		//}

		if ( hasIdentifier ) {
			update.addPrimaryKeyColumns( new String[] {identifierColumnName} );
		}
		else if ( hasIndex && !indexContainsFormula ) {
			update.addPrimaryKeyColumns( ArrayHelper.join( keyColumnNames, indexColumnNames ) );
		}
		else {
			update.addPrimaryKeyColumns( keyColumnNames );
			update.addPrimaryKeyColumns( elementColumnNames, elementColumnIsInPrimaryKey, elementColumnWriters );
		}

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( "update collection row " + getRole() );
		}

		return update.toStatementString();
	}

	@Override
	protected void doProcessQueuedOps(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session) {
		// nothing to do
	}

	/**
	 * Generate the SQL DELETE that deletes a particular row
	 */
	@Override
	protected String generateDeleteRowString() {
		final Delete delete = createDelete().setTableName( qualifiedTableName );

		if ( hasIdentifier ) {
			delete.addPrimaryKeyColumns( new String[] {identifierColumnName} );
		}
		else if ( hasIndex && !indexContainsFormula ) {
			delete.addPrimaryKeyColumns( ArrayHelper.join( keyColumnNames, indexColumnNames ) );
		}
		else {
			delete.addPrimaryKeyColumns( keyColumnNames );
			delete.addPrimaryKeyColumns( elementColumnNames, elementColumnIsInPrimaryKey, elementColumnWriters );
		}

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( "delete collection row " + getRole() );
		}

		return delete.toStatementString();
	}

	public boolean consumesEntityAlias() {
		return false;
	}

	public boolean consumesCollectionAlias() {
//		return !isOneToMany();
		return true;
	}

	public boolean isOneToMany() {
		return false;
	}

	@Override
	public boolean isManyToMany() {
		return elementType.isEntityType(); //instanceof AssociationType;
	}

	private BasicBatchKey updateBatchKey;

	@Override
	protected int doUpdateRows(Object id, PersistentCollection<?> collection, SharedSessionContractImplementor session)
			throws HibernateException {
		if ( ArrayHelper.isAllFalse( elementColumnIsSettable ) ) {
			return 0;
		}

		try {
			final Expectation expectation = Expectations.appropriateExpectation( getUpdateCheckStyle() );
			final boolean callable = isUpdateCallable();
			final int jdbcBatchSizeToUse = session.getConfiguredJdbcBatchSize();
			boolean useBatch = expectation.canBeBatched() && jdbcBatchSizeToUse > 1;
			final Iterator<?> entries = collection.entries( this );

			final List<Object> elements = new ArrayList<>();
			while ( entries.hasNext() ) {
				elements.add( entries.next() );
			}

			final String sql = getSQLUpdateRowString();
			int count = 0;
			if ( collection.isElementRemoved() ) {
				// the update should be done starting from the end to the list
				for ( int i = elements.size() - 1; i >= 0; i-- ) {
					count = doUpdateRow(
							id,
							collection,
							session,
							expectation,
							callable,
							useBatch,
							elements,
							sql,
							count,
							i
					);
				}
			}
			else {
				for ( int i = 0; i < elements.size(); i++ ) {
					count = doUpdateRow(
							id,
							collection,
							session,
							expectation,
							callable,
							useBatch,
							elements,
							sql,
							count,
							i
					);
				}
			}
			return count;
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not update collection rows: " + MessageHelper.collectionInfoString(
							this,
							collection,
							id,
							session
					),
					getSQLUpdateRowString()
			);
		}
	}

	private int doUpdateRow(
			Object id,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor session,
			Expectation expectation, boolean callable, boolean useBatch, List<?> elements, String sql, int count, int i)
			throws SQLException {
		PreparedStatement st;
		Object entry = elements.get( i );
		if ( collection.needsUpdating( entry, i, elementType ) ) {
			int offset = 1;

			if ( useBatch ) {
				if ( updateBatchKey == null ) {
					updateBatchKey = new BasicBatchKey(
							getRole() + "#UPDATE",
							expectation
					);
				}
				st = session
						.getJdbcCoordinator()
						.getBatch( updateBatchKey )
						.getBatchStatement( sql, callable );
			}
			else {
				st = session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql, callable );
			}

			try {
				offset += expectation.prepare( st );
				int loc = writeElement( st, collection.getElement( entry ), offset, session );
				if ( hasIdentifier ) {
					writeIdentifier( st, collection.getIdentifier( entry, i ), loc, session );
				}
				else {
					loc = writeKey( st, id, loc, session );
					if ( hasIndex && !indexContainsFormula ) {
						writeIndexToWhere( st, collection.getIndex( entry, i, this ), loc, session );
					}
					else {
						writeElementToWhere( st, collection.getSnapshotElement( entry, i ), loc, session );
					}
				}

				if ( useBatch ) {
					session.getJdbcCoordinator()
							.getBatch( updateBatchKey )
							.addToBatch();
				}
				else {
					expectation.verifyOutcome(
							session.getJdbcCoordinator().getResultSetReturn().executeUpdate(
									st
							), st, -1, sql
					);
				}
			}
			catch (SQLException sqle) {
				if ( useBatch ) {
					session.getJdbcCoordinator().abortBatch();
				}
				throw sqle;
			}
			finally {
				if ( !useBatch ) {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
					session.getJdbcCoordinator().afterStatementExecution();
				}
			}
			count++;
		}
		return count;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(TableGroup tableGroup) {
		return getFilterAliasGenerator( tableGroup.getPrimaryTableReference().getIdentificationVariable() );
	}

}
