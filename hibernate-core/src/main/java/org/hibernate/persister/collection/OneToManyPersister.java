/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SubselectFetch;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.loader.collection.BatchingCollectionInitializer;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.loader.collection.SubselectOneToManyLoader;
import org.hibernate.loader.entity.CollectionElementLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;
import org.hibernate.util.ArrayHelper;

/**
 * Collection persister for one-to-many associations.
 *
 * @author Gavin King
 */
public class OneToManyPersister extends AbstractCollectionPersister {

	private final boolean cascadeDeleteEnabled;
	private final boolean keyIsNullable;
	private final boolean keyIsUpdateable;

	protected boolean isRowDeleteEnabled() {
		return keyIsUpdateable && keyIsNullable;
	}

	protected boolean isRowInsertEnabled() {
		return keyIsUpdateable;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public OneToManyPersister(
			Collection collection,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			Configuration cfg,
			SessionFactoryImplementor factory) throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, cfg, factory );
		cascadeDeleteEnabled = collection.getKey().isCascadeDeleteEnabled() &&
				factory.getDialect().supportsCascadeDelete();
		keyIsNullable = collection.getKey().isNullable();
		keyIsUpdateable = collection.getKey().isUpdateable();
	}

	/**
	 * Generate the SQL UPDATE that updates all the foreign keys to null
	 */
	protected String generateDeleteString() {
		
		Update update = new Update( getDialect() )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames, "null" )
				.addPrimaryKeyColumns( keyColumnNames );
		
		if ( hasIndex && !indexContainsFormula ) update.addColumns( indexColumnNames, "null" );
		
		if ( hasWhere ) update.setWhere( sqlWhereString );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "delete one-to-many " + getRole() );
		}
		
		return update.toStatementString();
	}

	/**
	 * Generate the SQL UPDATE that updates a foreign key to a value
	 */
	protected String generateInsertRowString() {
		
		Update update = new Update( getDialect() )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames );
		
		if ( hasIndex && !indexContainsFormula ) update.addColumns( indexColumnNames );
		
		//identifier collections not supported for 1-to-many
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "create one-to-many row " + getRole() );
		}
		
		return update.addPrimaryKeyColumns( elementColumnNames, elementColumnWriters )
				.toStatementString();
	}

	/**
	 * Not needed for one-to-many association
	 */
	protected String generateUpdateRowString() {
		return null;
	}

	/**
	 * Generate the SQL UPDATE that updates a particular row's foreign
	 * key to null
	 */
	protected String generateDeleteRowString() {
		
		Update update = new Update( getDialect() )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames, "null" );
		
		if ( hasIndex && !indexContainsFormula ) update.addColumns( indexColumnNames, "null" );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "delete one-to-many row " + getRole() );
		}
		
		//use a combination of foreign key columns and pk columns, since
		//the ordering of removal and addition is not guaranteed when
		//a child moves from one parent to another
		String[] rowSelectColumnNames = ArrayHelper.join(keyColumnNames, elementColumnNames);
		return update.addPrimaryKeyColumns( rowSelectColumnNames )
				.toStatementString();
	}

	public boolean consumesEntityAlias() {
		return true;
	}
	public boolean consumesCollectionAlias() {
		return true;
	}

	public boolean isOneToMany() {
		return true;
	}

	public boolean isManyToMany() {
		return false;
	}

	protected int doUpdateRows(Serializable id, PersistentCollection collection, SessionImplementor session)
			throws HibernateException {

		// we finish all the "removes" first to take care of possible unique
		// constraints and so that we can take better advantage of batching
		
		try {
			int count = 0;
			if ( isRowDeleteEnabled() ) {
				boolean useBatch = true;
				PreparedStatement st = null;
				// update removed rows fks to null
				try {
					int i = 0;
	
					Iterator entries = collection.entries( this );
					int offset = 1;
					Expectation expectation = Expectations.NONE;
					while ( entries.hasNext() ) {
	
						Object entry = entries.next();
						if ( collection.needsUpdating( entry, i, elementType ) ) {  // will still be issued when it used to be null
							if ( st == null ) {
								String sql = getSQLDeleteRowString();
								if ( isDeleteCallable() ) {
									expectation = Expectations.appropriateExpectation( getDeleteCheckStyle() );
									useBatch = expectation.canBeBatched();
									st = useBatch
											? session.getBatcher().prepareBatchCallableStatement( sql )
								            : session.getBatcher().prepareCallableStatement( sql );
									offset += expectation.prepare( st );
								}
								else {
									st = session.getBatcher().prepareBatchStatement( getSQLDeleteRowString() );
								}
							}
							int loc = writeKey( st, id, offset, session );
							writeElementToWhere( st, collection.getSnapshotElement(entry, i), loc, session );
							if ( useBatch ) {
								session.getBatcher().addToBatch( expectation );
							}
							else {
								expectation.verifyOutcome( st.executeUpdate(), st, -1 );
							}
							count++;
						}
						i++;
					}
				}
				catch ( SQLException sqle ) {
					if ( useBatch ) {
						session.getBatcher().abortBatch( sqle );
					}
					throw sqle;
				}
				finally {
					if ( !useBatch ) {
						session.getBatcher().closeStatement( st );
					}
				}
			}
			
			if ( isRowInsertEnabled() ) {
				Expectation expectation = Expectations.appropriateExpectation( getInsertCheckStyle() );
				boolean callable = isInsertCallable();
				boolean useBatch = expectation.canBeBatched();
				String sql = getSQLInsertRowString();
				PreparedStatement st = null;
				// now update all changed or added rows fks
				try {
					int i = 0;
					Iterator entries = collection.entries( this );
					while ( entries.hasNext() ) {
						Object entry = entries.next();
						int offset = 1;
						if ( collection.needsUpdating( entry, i, elementType ) ) {
							if ( useBatch ) {
								if ( st == null ) {
									if ( callable ) {
										st = session.getBatcher().prepareBatchCallableStatement( sql );
									}
									else {
										st = session.getBatcher().prepareBatchStatement( sql );
									}
								}
							}
							else {
								if ( callable ) {
									st = session.getBatcher().prepareCallableStatement( sql );
								}
								else {
									st = session.getBatcher().prepareStatement( sql );
								}
							}

							offset += expectation.prepare( st );

							int loc = writeKey( st, id, offset, session );
							if ( hasIndex && !indexContainsFormula ) {
								loc = writeIndexToWhere( st, collection.getIndex( entry, i, this ), loc, session );
							}

							writeElementToWhere( st, collection.getElement( entry ), loc, session );

							if ( useBatch ) {
								session.getBatcher().addToBatch( expectation );
							}
							else {
								expectation.verifyOutcome( st.executeUpdate(), st, -1 );
							}
							count++;
						}
						i++;
					}
				}
				catch ( SQLException sqle ) {
					if ( useBatch ) {
						session.getBatcher().abortBatch( sqle );
					}
					throw sqle;
				}
				finally {
					if ( !useBatch ) {
						session.getBatcher().closeStatement( st );
					}
				}
			}

			return count;
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getSQLExceptionConverter(),
					sqle,
					"could not update collection rows: " + 
					MessageHelper.collectionInfoString( this, id, getFactory() ),
					getSQLInsertRowString()
			);
		}
	}

	public String selectFragment(
	        Joinable rhs,
	        String rhsAlias,
	        String lhsAlias,
	        String entitySuffix,
	        String collectionSuffix,
	        boolean includeCollectionColumns) {
		StringBuffer buf = new StringBuffer();
		if ( includeCollectionColumns ) {
//			buf.append( selectFragment( lhsAlias, "" ) )//ignore suffix for collection columns!
			buf.append( selectFragment( lhsAlias, collectionSuffix ) )
					.append( ", " );
		}
		OuterJoinLoadable ojl = ( OuterJoinLoadable ) getElementPersister();
		return buf.append( ojl.selectFragment( lhsAlias, entitySuffix ) )//use suffix for the entity columns
				.toString();
	}

	/**
	 * Create the <tt>OneToManyLoader</tt>
	 *
	 * @see org.hibernate.loader.collection.OneToManyLoader
	 */
	protected CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers) 
			throws MappingException {
		return BatchingCollectionInitializer.createBatchingOneToManyInitializer( this, batchSize, getFactory(), loadQueryInfluencers );
	}

	public String fromJoinFragment(String alias,
								   boolean innerJoin,
								   boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).fromJoinFragment( alias, innerJoin, includeSubclasses );
	}

	public String whereJoinFragment(String alias,
									boolean innerJoin,
									boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).whereJoinFragment( alias, innerJoin, includeSubclasses );
	}

	public String getTableName() {
		return ( ( Joinable ) getElementPersister() ).getTableName();
	}

	public String filterFragment(String alias) throws MappingException {
		String result = super.filterFragment( alias );
		if ( getElementPersister() instanceof Joinable ) {
			result += ( ( Joinable ) getElementPersister() ).oneToManyFilterFragment( alias );
		}
		return result;

	}

	protected CollectionInitializer createSubselectInitializer(SubselectFetch subselect, SessionImplementor session) {
		return new SubselectOneToManyLoader( 
				this,
				subselect.toSubselectString( getCollectionType().getLHSPropertyName() ),
				subselect.getResult(),
				subselect.getQueryParameters(),
				subselect.getNamedParameterLocMap(),
				session.getFactory(),
				session.getLoadQueryInfluencers()
			);
	}

	public Object getElementByIndex(Serializable key, Object index, SessionImplementor session, Object owner) {
		return new CollectionElementLoader( this, getFactory(), session.getLoadQueryInfluencers() )
				.loadElement( session, key, incrementIndexByBase(index) );
	}

}
