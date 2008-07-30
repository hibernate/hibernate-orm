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
package org.hibernate.loader.criteria;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * A <tt>Loader</tt> for <tt>Criteria</tt> queries. Note that criteria queries are
 * more like multi-object <tt>load()</tt>s than like HQL queries.
 *
 * @author Gavin King
 */
public class CriteriaLoader extends OuterJoinLoader {

	//TODO: this class depends directly upon CriteriaImpl, 
	//      in the impl package ... add a CriteriaImplementor 
	//      interface

	//NOTE: unlike all other Loaders, this one is NOT
	//      multithreaded, or cacheable!!

	private final CriteriaQueryTranslator translator;
	private final Set querySpaces;
	private final Type[] resultTypes;
	//the user visible aliases, which are unknown to the superclass,
	//these are not the actual "physical" SQL aliases
	private final String[] userAliases;

	public CriteriaLoader(
			final OuterJoinLoadable persister, 
			final SessionFactoryImplementor factory, 
			final CriteriaImpl criteria, 
			final String rootEntityName,
			final Map enabledFilters)
	throws HibernateException {
		super(factory, enabledFilters);

		translator = new CriteriaQueryTranslator(
				factory, 
				criteria, 
				rootEntityName, 
				CriteriaQueryTranslator.ROOT_SQL_ALIAS
			);

		querySpaces = translator.getQuerySpaces();
		
		CriteriaJoinWalker walker = new CriteriaJoinWalker(
				persister, 
				translator,
				factory, 
				criteria, 
				rootEntityName, 
				enabledFilters
			);

		initFromWalker(walker);
		
		userAliases = walker.getUserAliases();
		resultTypes = walker.getResultTypes();

		postInstantiate();

	}
	
	public ScrollableResults scroll(SessionImplementor session, ScrollMode scrollMode) 
	throws HibernateException {
		QueryParameters qp = translator.getQueryParameters();
		qp.setScrollMode(scrollMode);
		return scroll(qp, resultTypes, null, session);
	}

	public List list(SessionImplementor session) 
	throws HibernateException {
		return list( session, translator.getQueryParameters(), querySpaces, resultTypes );

	}

	protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
	throws SQLException, HibernateException {
		final Object[] result;
		final String[] aliases;
		if ( translator.hasProjection() ) {
			Type[] types = translator.getProjectedTypes();
			result = new Object[types.length];
			String[] columnAliases = translator.getProjectedColumnAliases();
			for ( int i=0; i<result.length; i++ ) {
				result[i] = types[i].nullSafeGet(rs, columnAliases[i], session, null);
			}
			aliases = translator.getProjectedAliases();
		}
		else {
			result = row;
			aliases = userAliases;
		}
		return translator.getRootCriteria().getResultTransformer()
				.transformTuple(result, aliases);
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	protected String applyLocks(String sqlSelectString, Map lockModes, Dialect dialect) throws QueryException {
		if ( lockModes == null || lockModes.isEmpty() ) {
			return sqlSelectString;
		}

		final Map aliasedLockModes = new HashMap();
		final Map keyColumnNames = dialect.forUpdateOfColumns() ? new HashMap() : null;
		final String[] drivingSqlAliases = getAliases();
		for ( int i = 0; i < drivingSqlAliases.length; i++ ) {
			final LockMode lockMode = ( LockMode ) lockModes.get( drivingSqlAliases[i] );
			if ( lockMode != null ) {
				final Lockable drivingPersister = ( Lockable ) getEntityPersisters()[i];
				final String rootSqlAlias = drivingPersister.getRootTableAlias( drivingSqlAliases[i] );
				aliasedLockModes.put( rootSqlAlias, lockMode );
				if ( keyColumnNames != null ) {
					keyColumnNames.put( rootSqlAlias, drivingPersister.getRootTableIdentifierColumnNames() );
				}
			}
		}
		return dialect.applyLocksToSql( sqlSelectString, aliasedLockModes, keyColumnNames );
	}

	protected LockMode[] getLockModes(Map lockModes) {
		final String[] entityAliases = getAliases();
		if ( entityAliases == null ) {
			return null;
		}
		final int size = entityAliases.length;
		LockMode[] lockModesArray = new LockMode[size];
		for ( int i=0; i<size; i++ ) {
			LockMode lockMode = (LockMode) lockModes.get( entityAliases[i] );
			lockModesArray[i] = lockMode==null ? LockMode.NONE : lockMode;
		}
		return lockModesArray;
	}

	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}
	
	protected List getResultList(List results, ResultTransformer resultTransformer) {
		return translator.getRootCriteria().getResultTransformer().transformList( results );
	}

}
