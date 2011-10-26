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
package org.hibernate.loader.custom.sql;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryNonScalarReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.ColumnEntityAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.custom.CollectionFetchReturn;
import org.hibernate.loader.custom.CollectionReturn;
import org.hibernate.loader.custom.ColumnCollectionAliases;
import org.hibernate.loader.custom.EntityFetchReturn;
import org.hibernate.loader.custom.FetchReturn;
import org.hibernate.loader.custom.NonScalarReturn;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.RootReturn;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SQLLoadable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Responsible for processing the series of {@link org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn returns}
 * defined by a {@link org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification} and
 * breaking them down into a series of {@link Return returns} for use within the
 * {@link org.hibernate.loader.custom.CustomLoader}.
 *
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class SQLQueryReturnProcessor {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       SQLQueryReturnProcessor.class.getName());

	private NativeSQLQueryReturn[] queryReturns;

//	private final List persisters = new ArrayList();

	private final Map alias2Return = new HashMap();
	private final Map alias2OwnerAlias = new HashMap();

	private final Map alias2Persister = new HashMap();
	private final Map alias2Suffix = new HashMap();

	private final Map alias2CollectionPersister = new HashMap();
	private final Map alias2CollectionSuffix = new HashMap();

	private final Map entityPropertyResultMaps = new HashMap();
	private final Map collectionPropertyResultMaps = new HashMap();

//	private final List scalarTypes = new ArrayList();
//	private final List scalarColumnAliases = new ArrayList();

	private final SessionFactoryImplementor factory;

//	private List collectionOwnerAliases = new ArrayList();
//	private List collectionAliases = new ArrayList();
//	private List collectionPersisters = new ArrayList();
//	private List collectionResults = new ArrayList();

	private int entitySuffixSeed = 0;
	private int collectionSuffixSeed = 0;


	public SQLQueryReturnProcessor(NativeSQLQueryReturn[] queryReturns, SessionFactoryImplementor factory) {
		this.queryReturns = queryReturns;
		this.factory = factory;
	}

	/*package*/ class ResultAliasContext {
		public SQLLoadable getEntityPersister(String alias) {
			return ( SQLLoadable ) alias2Persister.get( alias );
		}

		public SQLLoadableCollection getCollectionPersister(String alias) {
			return ( SQLLoadableCollection ) alias2CollectionPersister.get( alias );
		}

		public String getEntitySuffix(String alias) {
			return ( String ) alias2Suffix.get( alias );
		}

		public String getCollectionSuffix(String alias) {
			return ( String ) alias2CollectionSuffix.get ( alias );
		}

		public String getOwnerAlias(String alias) {
			return ( String ) alias2OwnerAlias.get( alias );
		}

		public Map getPropertyResultsMap(String alias) {
			return internalGetPropertyResultsMap( alias );
		}
	}

	private Map internalGetPropertyResultsMap(String alias) {
		NativeSQLQueryReturn rtn = ( NativeSQLQueryReturn ) alias2Return.get( alias );
		if ( rtn instanceof NativeSQLQueryNonScalarReturn ) {
			return ( ( NativeSQLQueryNonScalarReturn ) rtn ).getPropertyResultsMap();
		}
		else {
			return null;
		}
	}

	private boolean hasPropertyResultMap(String alias) {
		Map propertyMaps = internalGetPropertyResultsMap( alias );
		return propertyMaps != null && ! propertyMaps.isEmpty();
	}

	public ResultAliasContext process() {
		// first, break down the returns into maps keyed by alias
		// so that role returns can be more easily resolved to their owners
		for ( int i = 0; i < queryReturns.length; i++ ) {
			if ( queryReturns[i] instanceof NativeSQLQueryNonScalarReturn ) {
				NativeSQLQueryNonScalarReturn rtn = ( NativeSQLQueryNonScalarReturn ) queryReturns[i];
				alias2Return.put( rtn.getAlias(), rtn );
				if ( rtn instanceof NativeSQLQueryJoinReturn ) {
					NativeSQLQueryJoinReturn fetchReturn = ( NativeSQLQueryJoinReturn ) rtn;
					alias2OwnerAlias.put( fetchReturn.getAlias(), fetchReturn.getOwnerAlias() );
				}
			}
		}

		// Now, process the returns
		for ( int i = 0; i < queryReturns.length; i++ ) {
			processReturn( queryReturns[i] );
		}

		return new ResultAliasContext();
	}

	public List generateCustomReturns(boolean queryHadAliases) {
		List customReturns = new ArrayList();
		Map customReturnsByAlias = new HashMap();
		for ( int i = 0; i < queryReturns.length; i++ ) {
			if ( queryReturns[i] instanceof NativeSQLQueryScalarReturn ) {
				NativeSQLQueryScalarReturn rtn = ( NativeSQLQueryScalarReturn ) queryReturns[i];
				customReturns.add( new ScalarReturn( rtn.getType(), rtn.getColumnAlias() ) );
			}
			else if ( queryReturns[i] instanceof NativeSQLQueryRootReturn ) {
				NativeSQLQueryRootReturn rtn = ( NativeSQLQueryRootReturn ) queryReturns[i];
				String alias = rtn.getAlias();
				EntityAliases entityAliases;
				if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
					entityAliases = new DefaultEntityAliases(
							( Map ) entityPropertyResultMaps.get( alias ),
							( SQLLoadable ) alias2Persister.get( alias ),
							( String ) alias2Suffix.get( alias )
					);
				}
				else {
					entityAliases = new ColumnEntityAliases(
							( Map ) entityPropertyResultMaps.get( alias ),
							( SQLLoadable ) alias2Persister.get( alias ),
							( String ) alias2Suffix.get( alias )
					);
				}
				RootReturn customReturn = new RootReturn(
						alias,
						rtn.getReturnEntityName(),
						entityAliases,
						rtn.getLockMode()
				);
				customReturns.add( customReturn );
				customReturnsByAlias.put( rtn.getAlias(), customReturn );
			}
			else if ( queryReturns[i] instanceof NativeSQLQueryCollectionReturn ) {
				NativeSQLQueryCollectionReturn rtn = ( NativeSQLQueryCollectionReturn ) queryReturns[i];
				String alias = rtn.getAlias();
				SQLLoadableCollection persister = ( SQLLoadableCollection ) alias2CollectionPersister.get( alias );
				boolean isEntityElements = persister.getElementType().isEntityType();
				CollectionAliases collectionAliases;
				EntityAliases elementEntityAliases = null;
				if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
					collectionAliases = new GeneratedCollectionAliases(
							( Map ) collectionPropertyResultMaps.get( alias ),
							( SQLLoadableCollection ) alias2CollectionPersister.get( alias ),
							( String ) alias2CollectionSuffix.get( alias )
					);
					if ( isEntityElements ) {
						elementEntityAliases = new DefaultEntityAliases(
								( Map ) entityPropertyResultMaps.get( alias ),
								( SQLLoadable ) alias2Persister.get( alias ),
								( String ) alias2Suffix.get( alias )
						);
					}
				}
				else {
					collectionAliases = new ColumnCollectionAliases(
							( Map ) collectionPropertyResultMaps.get( alias ),
							( SQLLoadableCollection ) alias2CollectionPersister.get( alias )
					);
					if ( isEntityElements ) {
						elementEntityAliases = new ColumnEntityAliases(
								( Map ) entityPropertyResultMaps.get( alias ),
								( SQLLoadable ) alias2Persister.get( alias ),
								( String ) alias2Suffix.get( alias )
						);
					}
				}
				CollectionReturn customReturn = new CollectionReturn(
						alias,
						rtn.getOwnerEntityName(),
						rtn.getOwnerProperty(),
						collectionAliases,
				        elementEntityAliases,
						rtn.getLockMode()
				);
				customReturns.add( customReturn );
				customReturnsByAlias.put( rtn.getAlias(), customReturn );
			}
			else if ( queryReturns[i] instanceof NativeSQLQueryJoinReturn ) {
				NativeSQLQueryJoinReturn rtn = ( NativeSQLQueryJoinReturn ) queryReturns[i];
				String alias = rtn.getAlias();
				FetchReturn customReturn;
				NonScalarReturn ownerCustomReturn = ( NonScalarReturn ) customReturnsByAlias.get( rtn.getOwnerAlias() );
				if ( alias2CollectionPersister.containsKey( alias ) ) {
					SQLLoadableCollection persister = ( SQLLoadableCollection ) alias2CollectionPersister.get( alias );
					boolean isEntityElements = persister.getElementType().isEntityType();
					CollectionAliases collectionAliases;
					EntityAliases elementEntityAliases = null;
					if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
						collectionAliases = new GeneratedCollectionAliases(
								( Map ) collectionPropertyResultMaps.get( alias ),
								persister,
								( String ) alias2CollectionSuffix.get( alias )
						);
						if ( isEntityElements ) {
							elementEntityAliases = new DefaultEntityAliases(
									( Map ) entityPropertyResultMaps.get( alias ),
									( SQLLoadable ) alias2Persister.get( alias ),
									( String ) alias2Suffix.get( alias )
							);
						}
					}
					else {
						collectionAliases = new ColumnCollectionAliases(
								( Map ) collectionPropertyResultMaps.get( alias ),
								persister
						);
						if ( isEntityElements ) {
							elementEntityAliases = new ColumnEntityAliases(
									( Map ) entityPropertyResultMaps.get( alias ),
									( SQLLoadable ) alias2Persister.get( alias ),
									( String ) alias2Suffix.get( alias )
							);
						}
					}
					customReturn = new CollectionFetchReturn(
							alias,
							ownerCustomReturn,
							rtn.getOwnerProperty(),
							collectionAliases,
					        elementEntityAliases,
							rtn.getLockMode()
					);
				}
				else {
					EntityAliases entityAliases;
					if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
						entityAliases = new DefaultEntityAliases(
								( Map ) entityPropertyResultMaps.get( alias ),
								( SQLLoadable ) alias2Persister.get( alias ),
								( String ) alias2Suffix.get( alias )
						);
					}
					else {
						entityAliases = new ColumnEntityAliases(
								( Map ) entityPropertyResultMaps.get( alias ),
								( SQLLoadable ) alias2Persister.get( alias ),
								( String ) alias2Suffix.get( alias )
						);
					}
					customReturn = new EntityFetchReturn(
							alias,
							entityAliases,
							ownerCustomReturn,
							rtn.getOwnerProperty(),
							rtn.getLockMode()
					);
				}
				customReturns.add( customReturn );
				customReturnsByAlias.put( alias, customReturn );
			}
		}
		return customReturns;
	}

	private SQLLoadable getSQLLoadable(String entityName) throws MappingException {
		EntityPersister persister = factory.getEntityPersister( entityName );
		if ( !(persister instanceof SQLLoadable) ) {
			throw new MappingException( "class persister is not SQLLoadable: " + entityName );
		}
		return (SQLLoadable) persister;
	}

	private String generateEntitySuffix() {
		return BasicLoader.generateSuffixes( entitySuffixSeed++, 1 )[0];
	}

	private String generateCollectionSuffix() {
		return collectionSuffixSeed++ + "__";
	}

	private void processReturn(NativeSQLQueryReturn rtn) {
		if ( rtn instanceof NativeSQLQueryScalarReturn ) {
			processScalarReturn( ( NativeSQLQueryScalarReturn ) rtn );
		}
		else if ( rtn instanceof NativeSQLQueryRootReturn ) {
			processRootReturn( ( NativeSQLQueryRootReturn ) rtn );
		}
		else if ( rtn instanceof NativeSQLQueryCollectionReturn ) {
			processCollectionReturn( ( NativeSQLQueryCollectionReturn ) rtn );
		}
		else {
			processJoinReturn( ( NativeSQLQueryJoinReturn ) rtn );
		}
	}

	private void processScalarReturn(NativeSQLQueryScalarReturn typeReturn) {
//		scalarColumnAliases.add( typeReturn.getColumnAlias() );
//		scalarTypes.add( typeReturn.getType() );
	}

	private void processRootReturn(NativeSQLQueryRootReturn rootReturn) {
		if ( alias2Persister.containsKey( rootReturn.getAlias() ) ) {
			// already been processed...
			return;
		}

		SQLLoadable persister = getSQLLoadable( rootReturn.getReturnEntityName() );
		addPersister( rootReturn.getAlias(), rootReturn.getPropertyResultsMap(), persister );
	}

	/**
	 * @param propertyResult
	 * @param persister
	 */
	private void addPersister(String alias, Map propertyResult, SQLLoadable persister) {
		alias2Persister.put( alias, persister );
		String suffix = generateEntitySuffix();
		LOG.tracev( "Mapping alias [{0}] to entity-suffix [{1}]", alias, suffix );
		alias2Suffix.put( alias, suffix );
		entityPropertyResultMaps.put( alias, propertyResult );
	}

	private void addCollection(String role, String alias, Map propertyResults) {
		SQLLoadableCollection collectionPersister = ( SQLLoadableCollection ) factory.getCollectionPersister( role );
		alias2CollectionPersister.put( alias, collectionPersister );
		String suffix = generateCollectionSuffix();
		LOG.tracev( "Mapping alias [{0}] to collection-suffix [{1}]", alias, suffix );
		alias2CollectionSuffix.put( alias, suffix );
		collectionPropertyResultMaps.put( alias, propertyResults );

		if ( collectionPersister.isOneToMany() || collectionPersister.isManyToMany() ) {
			SQLLoadable persister = ( SQLLoadable ) collectionPersister.getElementPersister();
			addPersister( alias, filter( propertyResults ), persister );
		}
	}

	private Map filter(Map propertyResults) {
		Map result = new HashMap( propertyResults.size() );

		String keyPrefix = "element.";

		Iterator iter = propertyResults.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry element = ( Map.Entry ) iter.next();
			String path = ( String ) element.getKey();
			if ( path.startsWith( keyPrefix ) ) {
				result.put( path.substring( keyPrefix.length() ), element.getValue() );
			}
		}

		return result;
	}

	private void processCollectionReturn(NativeSQLQueryCollectionReturn collectionReturn) {
		// we are initializing an owned collection
		//collectionOwners.add( new Integer(-1) );
//		collectionOwnerAliases.add( null );
		String role = collectionReturn.getOwnerEntityName() + '.' + collectionReturn.getOwnerProperty();
		addCollection(
				role,
				collectionReturn.getAlias(),
				collectionReturn.getPropertyResultsMap()
		);
	}

	private void processJoinReturn(NativeSQLQueryJoinReturn fetchReturn) {
		String alias = fetchReturn.getAlias();
//		if ( alias2Persister.containsKey( alias ) || collectionAliases.contains( alias ) ) {
		if ( alias2Persister.containsKey( alias ) || alias2CollectionPersister.containsKey( alias ) ) {
			// already been processed...
			return;
		}

		String ownerAlias = fetchReturn.getOwnerAlias();

		// Make sure the owner alias is known...
		if ( !alias2Return.containsKey( ownerAlias ) ) {
			throw new HibernateException( "Owner alias [" + ownerAlias + "] is unknown for alias [" + alias + "]" );
		}

		// If this return's alias has not been processed yet, do so b4 further processing of this return
		if ( !alias2Persister.containsKey( ownerAlias ) ) {
			NativeSQLQueryNonScalarReturn ownerReturn = ( NativeSQLQueryNonScalarReturn ) alias2Return.get(ownerAlias);
			processReturn( ownerReturn );
		}

		SQLLoadable ownerPersister = ( SQLLoadable ) alias2Persister.get( ownerAlias );
		Type returnType = ownerPersister.getPropertyType( fetchReturn.getOwnerProperty() );

		if ( returnType.isCollectionType() ) {
			String role = ownerPersister.getEntityName() + '.' + fetchReturn.getOwnerProperty();
			addCollection( role, alias, fetchReturn.getPropertyResultsMap() );
//			collectionOwnerAliases.add( ownerAlias );
		}
		else if ( returnType.isEntityType() ) {
			EntityType eType = ( EntityType ) returnType;
			String returnEntityName = eType.getAssociatedEntityName();
			SQLLoadable persister = getSQLLoadable( returnEntityName );
			addPersister( alias, fetchReturn.getPropertyResultsMap(), persister );
		}

	}

//	public List getCollectionAliases() {
//		return collectionAliases;
//	}
//
//	/*public List getCollectionOwners() {
//		return collectionOwners;
//	}*/
//
//	public List getCollectionOwnerAliases() {
//		return collectionOwnerAliases;
//	}
//
//	public List getCollectionPersisters() {
//		return collectionPersisters;
//	}
//
//	public Map getAlias2Persister() {
//		return alias2Persister;
//	}
//
//	/*public boolean isCollectionInitializer() {
//		return isCollectionInitializer;
//	}*/
//
////	public List getPersisters() {
////		return persisters;
////	}
//
//	public Map getAlias2OwnerAlias() {
//		return alias2OwnerAlias;
//	}
//
//	public List getScalarTypes() {
//		return scalarTypes;
//	}
//	public List getScalarColumnAliases() {
//		return scalarColumnAliases;
//	}
//
//	public List getPropertyResults() {
//		return propertyResults;
//	}
//
//	public List getCollectionPropertyResults() {
//		return collectionResults;
//	}
//
//
//	public Map getAlias2Return() {
//		return alias2Return;
//	}
}
