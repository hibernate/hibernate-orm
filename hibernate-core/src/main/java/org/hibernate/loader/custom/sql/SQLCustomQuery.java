/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.loader.custom.sql;
import java.io.Serializable;
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
import org.hibernate.loader.custom.Return;
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
public class SQLCustomQuery implements CustomQuery, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, SQLCustomQuery.class.getName() );

	private final String sql;
	private final Set querySpaces = new HashSet();
	private final Map namedParameterBindPoints = new HashMap();
	private final List<Return> customQueryReturns = new ArrayList<Return>();

	@Override
	public String getSQL() {
		return sql;
	}
	@Override
	public Set getQuerySpaces() {
		return querySpaces;
	}
	@Override
	public Map getNamedParameterBindPoints() {
		return namedParameterBindPoints;
	}
	@Override
	public List<Return> getCustomQueryReturns() {
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

		SQLQueryParser parser = new SQLQueryParser( sqlQuery, new ParserContext( aliasContext ), factory );
		this.sql = parser.process();

		this.namedParameterBindPoints.putAll( parser.getNamedParameters() );
		this.customQueryReturns.addAll( processor.generateCustomReturns( parser.queryHasAliases() ) );
		if ( additionalQuerySpaces != null ) {
			querySpaces.addAll( additionalQuerySpaces );
		}
	}


	private static class ParserContext implements SQLQueryParser.ParserContext {

		private final SQLQueryReturnProcessor.ResultAliasContext aliasContext;

		public ParserContext(SQLQueryReturnProcessor.ResultAliasContext aliasContext) {
			this.aliasContext = aliasContext;
		}
		@Override
		public boolean isEntityAlias(String alias) {
			return getEntityPersisterByAlias( alias ) != null;
		}
		@Override
		public SQLLoadable getEntityPersisterByAlias(String alias) {
			return aliasContext.getEntityPersister( alias );
		}
		@Override
		public String getEntitySuffixByAlias(String alias) {
			return aliasContext.getEntitySuffix( alias );
		}
		@Override
		public boolean isCollectionAlias(String alias) {
			return getCollectionPersisterByAlias( alias ) != null;
		}
		@Override
		public SQLLoadableCollection getCollectionPersisterByAlias(String alias) {
			return aliasContext.getCollectionPersister( alias );
		}
		@Override
		public String getCollectionSuffixByAlias(String alias) {
			return aliasContext.getCollectionSuffix( alias );
		}
		@Override
		public Map<String, String[]> getPropertyResultsMapByAlias(String alias) {
			return aliasContext.getPropertyResultsMap( alias );
		}
	}
}
