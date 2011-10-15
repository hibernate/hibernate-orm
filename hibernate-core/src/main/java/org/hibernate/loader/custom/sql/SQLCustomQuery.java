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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.SQLLoadable;

/**
 * Implements Hibernate's built-in support for native SQL queries.
 * <p/>
 * This support is built on top of the notion of "custom queries"...
 *
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class SQLCustomQuery implements CustomQuery {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, SQLCustomQuery.class.getName() );

	private final String sql;
	private final Set querySpaces = new HashSet();
	private final Map namedParameterBindPoints = new HashMap();
	private final List customQueryReturns = new ArrayList();


	public String getSQL() {
		return sql;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	public Map getNamedParameterBindPoints() {
		return namedParameterBindPoints;
	}

	public List getCustomQueryReturns() {
		return customQueryReturns;
	}

	public SQLCustomQuery(
			final String sqlQuery,
			final NativeSQLQueryReturn[] queryReturns,
			final Collection additionalQuerySpaces,
			final SessionFactoryImplementor factory) throws HibernateException {

		LOG.tracev( "Starting processing of sql query [{0}]", sqlQuery );
		SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(queryReturns, factory);
		SQLQueryReturnProcessor.ResultAliasContext aliasContext = processor.process();


//		Map[] propertyResultMaps =  (Map[]) processor.getPropertyResults().toArray( new Map[0] );
//		Map[] collectionResultMaps =  (Map[]) processor.getCollectionPropertyResults().toArray( new Map[0] );
//
//		List collectionSuffixes = new ArrayList();
//		List collectionOwnerAliases = processor.getCollectionOwnerAliases();
//		List collectionPersisters = processor.getCollectionPersisters();
//		int size = collectionPersisters.size();
//		if (size!=0) {
//			collectionOwners = new int[size];
//			collectionRoles = new String[size];
//			//collectionDescriptors = new CollectionAliases[size];
//			for ( int i=0; i<size; i++ ) {
//				CollectionPersister collectionPersister = (CollectionPersister) collectionPersisters.get(i);
//				collectionRoles[i] = ( collectionPersister ).getRole();
//				collectionOwners[i] = processor.getAliases().indexOf( collectionOwnerAliases.get(i) );
//				String suffix = i + "__";
//				collectionSuffixes.add(suffix);
//				//collectionDescriptors[i] = new GeneratedCollectionAliases( collectionResultMaps[i], collectionPersister, suffix );
//			}
//		}
//		else {
//			collectionRoles = null;
//			//collectionDescriptors = null;
//			collectionOwners = null;
//		}
//
//		String[] aliases = ArrayHelper.toStringArray( processor.getAliases() );
//		String[] collAliases = ArrayHelper.toStringArray( processor.getCollectionAliases() );
//		String[] collSuffixes = ArrayHelper.toStringArray(collectionSuffixes);
//
//		SQLLoadable[] entityPersisters = (SQLLoadable[]) processor.getPersisters().toArray( new SQLLoadable[0] );
//		SQLLoadableCollection[] collPersisters = (SQLLoadableCollection[]) collectionPersisters.toArray( new SQLLoadableCollection[0] );
//        lockModes = (LockMode[]) processor.getLockModes().toArray( new LockMode[0] );
//
//        scalarColumnAliases = ArrayHelper.toStringArray( processor.getScalarColumnAliases() );
//		scalarTypes = ArrayHelper.toTypeArray( processor.getScalarTypes() );
//
//		// need to match the "sequence" of what we return. scalar first, entity last.
//		returnAliases = ArrayHelper.join(scalarColumnAliases, aliases);
//
//		String[] suffixes = BasicLoader.generateSuffixes(entityPersisters.length);

		SQLQueryParser parser = new SQLQueryParser( sqlQuery, new ParserContext( aliasContext ), factory );
		this.sql = parser.process();
		this.namedParameterBindPoints.putAll( parser.getNamedParameters() );

//		SQLQueryParser parser = new SQLQueryParser(
//				sqlQuery,
//				processor.getAlias2Persister(),
//				processor.getAlias2Return(),
//				aliases,
//				collAliases,
//				collPersisters,
//				suffixes,
//				collSuffixes
//		);
//
//		sql = parser.process();
//
//		namedParameterBindPoints = parser.getNamedParameters();


		customQueryReturns.addAll( processor.generateCustomReturns( parser.queryHasAliases() ) );

//		// Populate entityNames, entityDescrptors and querySpaces
//		entityNames = new String[entityPersisters.length];
//		entityDescriptors = new EntityAliases[entityPersisters.length];
//		for (int i = 0; i < entityPersisters.length; i++) {
//			SQLLoadable persister = entityPersisters[i];
//			//alias2Persister.put( aliases[i], persister );
//			//TODO: Does not consider any other tables referenced in the query
//			ArrayHelper.addAll( querySpaces, persister.getQuerySpaces() );
//			entityNames[i] = persister.getEntityName();
//			if ( parser.queryHasAliases() ) {
//				entityDescriptors[i] = new DefaultEntityAliases(
//						propertyResultMaps[i],
//						entityPersisters[i],
//						suffixes[i]
//					);
//			}
//			else {
//				entityDescriptors[i] = new ColumnEntityAliases(
//						propertyResultMaps[i],
//						entityPersisters[i],
//						suffixes[i]
//					);
//			}
//		}
		if ( additionalQuerySpaces != null ) {
			querySpaces.addAll( additionalQuerySpaces );
		}

//		if (size!=0) {
//			collectionDescriptors = new CollectionAliases[size];
//			for ( int i=0; i<size; i++ ) {
//				CollectionPersister collectionPersister = (CollectionPersister) collectionPersisters.get(i);
//				String suffix = i + "__";
//				if( parser.queryHasAliases() ) {
//					collectionDescriptors[i] = new GeneratedCollectionAliases( collectionResultMaps[i], collectionPersister, suffix );
//				} else {
//					collectionDescriptors[i] = new ColumnCollectionAliases( collectionResultMaps[i], (SQLLoadableCollection) collectionPersister );
//				}
//			}
//		}
//		else {
//			collectionDescriptors = null;
//		}
//
//
//		// Resolve owners
//		Map alias2OwnerAlias = processor.getAlias2OwnerAlias();
//		int[] ownersArray = new int[entityPersisters.length];
//		for ( int j=0; j < aliases.length; j++ ) {
//			String ownerAlias = (String) alias2OwnerAlias.get( aliases[j] );
//			if ( StringHelper.isNotEmpty(ownerAlias) ) {
//				ownersArray[j] =  processor.getAliases().indexOf( ownerAlias );
//			}
//			else {
//				ownersArray[j] = -1;
//			}
//		}
//		if ( ArrayHelper.isAllNegative(ownersArray) ) {
//			ownersArray = null;
//		}
//		this.entityOwners = ownersArray;

	}


	private static class ParserContext implements SQLQueryParser.ParserContext {

		private final SQLQueryReturnProcessor.ResultAliasContext aliasContext;

		public ParserContext(SQLQueryReturnProcessor.ResultAliasContext aliasContext) {
			this.aliasContext = aliasContext;
		}

		public boolean isEntityAlias(String alias) {
			return getEntityPersisterByAlias( alias ) != null;
		}

		public SQLLoadable getEntityPersisterByAlias(String alias) {
			return aliasContext.getEntityPersister( alias );
		}

		public String getEntitySuffixByAlias(String alias) {
			return aliasContext.getEntitySuffix( alias );
		}

		public boolean isCollectionAlias(String alias) {
			return getCollectionPersisterByAlias( alias ) != null;
		}

		public SQLLoadableCollection getCollectionPersisterByAlias(String alias) {
			return aliasContext.getCollectionPersister( alias );
		}

		public String getCollectionSuffixByAlias(String alias) {
			return aliasContext.getCollectionSuffix( alias );
		}

		public Map getPropertyResultsMapByAlias(String alias) {
			return aliasContext.getPropertyResultsMap( alias );
		}
	}
}
