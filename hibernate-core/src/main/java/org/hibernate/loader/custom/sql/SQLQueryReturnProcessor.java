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
import org.hibernate.internal.util.collections.CollectionHelper;
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
	private final Map<String, NativeSQLQueryReturn> alias2Return = new HashMap<String, NativeSQLQueryReturn>();
	private final Map<String, String> alias2OwnerAlias = new HashMap<String, String>();

	private final Map<String, SQLLoadable> alias2Persister = new HashMap<String, SQLLoadable>();
	private final Map<String, String> alias2Suffix = new HashMap<String, String>();

	private final Map<String, SQLLoadableCollection> alias2CollectionPersister = new HashMap<String, SQLLoadableCollection>();
	private final Map<String, String> alias2CollectionSuffix = new HashMap<String, String>();

	private final Map<String, Map<String, String[]>> entityPropertyResultMaps = new HashMap<String, Map<String, String[]>>();
	private final Map<String, Map<String, String[]>> collectionPropertyResultMaps = new HashMap<String, Map<String, String[]>>();
	private final SessionFactoryImplementor factory;
	private int entitySuffixSeed = 0;
	private int collectionSuffixSeed = 0;


	public SQLQueryReturnProcessor(NativeSQLQueryReturn[] queryReturns, SessionFactoryImplementor factory) {
		this.queryReturns = queryReturns;
		this.factory = factory;
	}

	/*package*/ class ResultAliasContext {
		public SQLLoadable getEntityPersister(String alias) {
			return alias2Persister.get( alias );
		}

		public SQLLoadableCollection getCollectionPersister(String alias) {
			return alias2CollectionPersister.get( alias );
		}

		public String getEntitySuffix(String alias) {
			return alias2Suffix.get( alias );
		}

		public String getCollectionSuffix(String alias) {
			return alias2CollectionSuffix.get ( alias );
		}

		public String getOwnerAlias(String alias) {
			return alias2OwnerAlias.get( alias );
		}

		public Map<String, String[]> getPropertyResultsMap(String alias) {
			return internalGetPropertyResultsMap( alias );
		}
	}

	private Map<String, String[]> internalGetPropertyResultsMap(String alias) {
		final NativeSQLQueryReturn rtn = alias2Return.get( alias );
		return rtn.getNature() != NativeSQLQueryReturn.Nature.SCALAR ? NativeSQLQueryNonScalarReturn.class.cast( rtn ).getPropertyResultsMap() : null;
	}

	private boolean hasPropertyResultMap(String alias) {
		Map propertyMaps = internalGetPropertyResultsMap( alias );
		return CollectionHelper.isNotEmpty( propertyMaps );
	}

	public ResultAliasContext process() {
		// first, break down the returns into maps keyed by alias
		// so that role returns can be more easily resolved to their owners
		for ( NativeSQLQueryReturn queryReturn : queryReturns ) {
			if ( queryReturn instanceof NativeSQLQueryNonScalarReturn ) {
				NativeSQLQueryNonScalarReturn rtn = (NativeSQLQueryNonScalarReturn) queryReturn;
				alias2Return.put( rtn.getAlias(), rtn );
				if ( rtn instanceof NativeSQLQueryJoinReturn ) {
					NativeSQLQueryJoinReturn fetchReturn = (NativeSQLQueryJoinReturn) rtn;
					alias2OwnerAlias.put( fetchReturn.getAlias(), fetchReturn.getOwnerAlias() );
				}
			}
		}

		// Now, process the returns
		for ( NativeSQLQueryReturn queryReturn : queryReturns ) {
			processReturn( queryReturn );
		}

		return new ResultAliasContext();
	}

	public List<Return> generateCustomReturns(boolean queryHadAliases) {
		List<Return> customReturns = new ArrayList<Return>();
		Map<String,Return> customReturnsByAlias = new HashMap<String,Return>();
		for ( NativeSQLQueryReturn queryReturn : queryReturns ) {
			if ( queryReturn instanceof NativeSQLQueryScalarReturn ) {
				NativeSQLQueryScalarReturn rtn = (NativeSQLQueryScalarReturn) queryReturn;
				customReturns.add( new ScalarReturn( rtn.getType(), rtn.getColumnAlias() ) );
			}
			else if ( queryReturn instanceof NativeSQLQueryRootReturn ) {
				NativeSQLQueryRootReturn rtn = (NativeSQLQueryRootReturn) queryReturn;
				String alias = rtn.getAlias();
				EntityAliases entityAliases;
				if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
					entityAliases = new DefaultEntityAliases(
							(Map) entityPropertyResultMaps.get( alias ),
							(SQLLoadable) alias2Persister.get( alias ),
							(String) alias2Suffix.get( alias )
					);
				}
				else {
					entityAliases = new ColumnEntityAliases(
							(Map) entityPropertyResultMaps.get( alias ),
							(SQLLoadable) alias2Persister.get( alias ),
							(String) alias2Suffix.get( alias )
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
			else if ( queryReturn instanceof NativeSQLQueryCollectionReturn ) {
				NativeSQLQueryCollectionReturn rtn = (NativeSQLQueryCollectionReturn) queryReturn;
				String alias = rtn.getAlias();
				SQLLoadableCollection persister = (SQLLoadableCollection) alias2CollectionPersister.get( alias );
				boolean isEntityElements = persister.getElementType().isEntityType();
				CollectionAliases collectionAliases;
				EntityAliases elementEntityAliases = null;
				if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
					collectionAliases = new GeneratedCollectionAliases(
							(Map) collectionPropertyResultMaps.get( alias ),
							(SQLLoadableCollection) alias2CollectionPersister.get( alias ),
							(String) alias2CollectionSuffix.get( alias )
					);
					if ( isEntityElements ) {
						elementEntityAliases = new DefaultEntityAliases(
								(Map) entityPropertyResultMaps.get( alias ),
								(SQLLoadable) alias2Persister.get( alias ),
								(String) alias2Suffix.get( alias )
						);
					}
				}
				else {
					collectionAliases = new ColumnCollectionAliases(
							(Map) collectionPropertyResultMaps.get( alias ),
							(SQLLoadableCollection) alias2CollectionPersister.get( alias )
					);
					if ( isEntityElements ) {
						elementEntityAliases = new ColumnEntityAliases(
								(Map) entityPropertyResultMaps.get( alias ),
								(SQLLoadable) alias2Persister.get( alias ),
								(String) alias2Suffix.get( alias )
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
			else if ( queryReturn instanceof NativeSQLQueryJoinReturn ) {
				NativeSQLQueryJoinReturn rtn = (NativeSQLQueryJoinReturn) queryReturn;
				String alias = rtn.getAlias();
				FetchReturn customReturn;
				NonScalarReturn ownerCustomReturn = (NonScalarReturn) customReturnsByAlias.get( rtn.getOwnerAlias() );
				if ( alias2CollectionPersister.containsKey( alias ) ) {
					SQLLoadableCollection persister = (SQLLoadableCollection) alias2CollectionPersister.get( alias );
					boolean isEntityElements = persister.getElementType().isEntityType();
					CollectionAliases collectionAliases;
					EntityAliases elementEntityAliases = null;
					if ( queryHadAliases || hasPropertyResultMap( alias ) ) {
						collectionAliases = new GeneratedCollectionAliases(
								(Map) collectionPropertyResultMaps.get( alias ),
								persister,
								(String) alias2CollectionSuffix.get( alias )
						);
						if ( isEntityElements ) {
							elementEntityAliases = new DefaultEntityAliases(
									(Map) entityPropertyResultMaps.get( alias ),
									(SQLLoadable) alias2Persister.get( alias ),
									(String) alias2Suffix.get( alias )
							);
						}
					}
					else {
						collectionAliases = new ColumnCollectionAliases(
								(Map) collectionPropertyResultMaps.get( alias ),
								persister
						);
						if ( isEntityElements ) {
							elementEntityAliases = new ColumnEntityAliases(
									(Map) entityPropertyResultMaps.get( alias ),
									(SQLLoadable) alias2Persister.get( alias ),
									(String) alias2Suffix.get( alias )
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
								(Map) entityPropertyResultMaps.get( alias ),
								(SQLLoadable) alias2Persister.get( alias ),
								(String) alias2Suffix.get( alias )
						);
					}
					else {
						entityAliases = new ColumnEntityAliases(
								(Map) entityPropertyResultMaps.get( alias ),
								(SQLLoadable) alias2Persister.get( alias ),
								(String) alias2Suffix.get( alias )
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
		if ( !SQLLoadable.class.isInstance( persister ) ) {
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
		switch ( rtn.getNature() ) {
			case SCALAR:
				processScalarReturn( (NativeSQLQueryScalarReturn) rtn );
				break;
			case ROOT:
				processRootReturn( (NativeSQLQueryRootReturn) rtn );
				break;
			case COLLECTION:
				processCollectionReturn( (NativeSQLQueryCollectionReturn) rtn );
				break;
			case JOIN:
				processJoinReturn( (NativeSQLQueryJoinReturn) rtn );
				break;
			default:
				throw new HibernateException( "unkonwn Query Return nature of " + rtn.getNature() );
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

	private void addPersister(String alias, Map propertyResult, SQLLoadable persister) {
		alias2Persister.put( alias, persister );
		final String suffix = generateEntitySuffix();
		LOG.tracev( "Mapping alias [{0}] to entity-suffix [{1}]", alias, suffix );
		alias2Suffix.put( alias, suffix );
		entityPropertyResultMaps.put( alias, propertyResult );
	}

	private void addCollection(String role, String alias, Map<String, String[]> propertyResults) {
		SQLLoadableCollection collectionPersister = ( SQLLoadableCollection ) factory.getCollectionPersister( role );
		alias2CollectionPersister.put( alias, collectionPersister );
		final String suffix = generateCollectionSuffix();
		LOG.tracev( "Mapping alias [{0}] to collection-suffix [{1}]", alias, suffix );
		alias2CollectionSuffix.put( alias, suffix );
		collectionPropertyResultMaps.put( alias, propertyResults );

		if ( collectionPersister.isOneToMany() || collectionPersister.isManyToMany() ) {
			SQLLoadable persister = ( SQLLoadable ) collectionPersister.getElementPersister();
			addPersister( alias, filter( propertyResults ), persister );
		}
	}

	private Map<String, String[]> filter(Map<String, String[]> propertyResults) {
		Map<String, String[]> result = new HashMap<String, String[]>( propertyResults.size() );
		final String keyPrefix = "element.";
		final int length = keyPrefix.length();
		for ( final String path : propertyResults.keySet() ) {
			if ( path.startsWith( keyPrefix ) ) {
				String [] value = propertyResults.get( path );
				result.put( path.substring( length ), value );
			}
			//todo add rest into result map w/o modification??
		}
		return result;
	}

	private void processCollectionReturn(NativeSQLQueryCollectionReturn collectionReturn) {
		// we are initializing an owned collection
		//collectionOwners.add( new Integer(-1) );
//		collectionOwnerAliases.add( null );
		final String role = collectionReturn.getOwnerEntityName() + '.' + collectionReturn.getOwnerProperty();
		addCollection(
				role,
				collectionReturn.getAlias(),
				collectionReturn.getPropertyResultsMap()
		);
	}

	private void processJoinReturn(NativeSQLQueryJoinReturn fetchReturn) {
		final String alias = fetchReturn.getAlias();
		if ( alias2Persister.containsKey( alias ) || alias2CollectionPersister.containsKey( alias ) ) {
			// already been processed...
			return;
		}

		final String ownerAlias = fetchReturn.getOwnerAlias();

		// Make sure the owner alias is known...
		if ( !alias2Return.containsKey( ownerAlias ) ) {
			throw new HibernateException( "Owner alias [" + ownerAlias + "] is unknown for alias [" + alias + "]" );
		}

		// If this return's alias has not been processed yet, do so b4 further processing of this return
		if ( !alias2Persister.containsKey( ownerAlias ) ) {
			NativeSQLQueryNonScalarReturn ownerReturn = ( NativeSQLQueryNonScalarReturn ) alias2Return.get(ownerAlias);
			processReturn( ownerReturn );
		}

		SQLLoadable ownerPersister = alias2Persister.get( ownerAlias );
		Type returnType = ownerPersister.getPropertyType( fetchReturn.getOwnerProperty() );

		if ( returnType.isCollectionType() ) {
			String role = ownerPersister.getEntityName() + '.' + fetchReturn.getOwnerProperty();
			addCollection( role, alias, fetchReturn.getPropertyResultsMap() );
		}
		else if ( returnType.isEntityType() ) {
			EntityType eType = ( EntityType ) returnType;
			String returnEntityName = eType.getAssociatedEntityName();
			SQLLoadable persister = getSQLLoadable( returnEntityName );
			addPersister( alias, fetchReturn.getPropertyResultsMap(), persister );
		}

	}
}
