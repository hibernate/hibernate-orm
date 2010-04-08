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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.LinkedHashMap;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.LockOptions;
import org.hibernate.criterion.EnhancedProjection;
import org.hibernate.hql.ast.util.SessionFactoryHelper;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projection;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.type.NullableType;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * @author Gavin King
 */
public class CriteriaQueryTranslator implements CriteriaQuery {

	public static final String ROOT_SQL_ALIAS = Criteria.ROOT_ALIAS + '_';

	private CriteriaQuery outerQueryTranslator;

	private final CriteriaImpl rootCriteria;
	private final String rootEntityName;
	private final String rootSQLAlias;
	private int aliasCount = 0;

	private final Map criteriaEntityNames = new LinkedHashMap();
	private final Map criteriaSQLAliasMap = new HashMap();
	private final Map aliasCriteriaMap = new HashMap();
	private final Map associationPathCriteriaMap = new LinkedHashMap();
	private final Map associationPathJoinTypesMap = new LinkedHashMap();
	private final Map withClauseMap = new HashMap();
	
	private final SessionFactoryImplementor sessionFactory;

	public CriteriaQueryTranslator(
			final SessionFactoryImplementor factory,
	        final CriteriaImpl criteria,
	        final String rootEntityName,
	        final String rootSQLAlias,
	        CriteriaQuery outerQuery) throws HibernateException {
		this( factory, criteria, rootEntityName, rootSQLAlias );
		outerQueryTranslator = outerQuery;
	}

	public CriteriaQueryTranslator(
			final SessionFactoryImplementor factory,
	        final CriteriaImpl criteria,
	        final String rootEntityName,
	        final String rootSQLAlias) throws HibernateException {
		this.rootCriteria = criteria;
		this.rootEntityName = rootEntityName;
		this.sessionFactory = factory;
		this.rootSQLAlias = rootSQLAlias;
		createAliasCriteriaMap();
		createAssociationPathCriteriaMap();
		createCriteriaEntityNameMap();
		createCriteriaSQLAliasMap();
	}

	public String generateSQLAlias() {
		return StringHelper.generateAlias( Criteria.ROOT_ALIAS, aliasCount ) + '_';
	}

	public String getRootSQLALias() {
		return rootSQLAlias;
	}

	private Criteria getAliasedCriteria(String alias) {
		return ( Criteria ) aliasCriteriaMap.get( alias );
	}

	public boolean isJoin(String path) {
		return associationPathCriteriaMap.containsKey( path );
	}

	public int getJoinType(String path) {
		Integer result = ( Integer ) associationPathJoinTypesMap.get( path );
		return ( result == null ? Criteria.INNER_JOIN : result.intValue() );
	}

	public Criteria getCriteria(String path) {
		return ( Criteria ) associationPathCriteriaMap.get( path );
	}

	public Set getQuerySpaces() {
		Set result = new HashSet();
		Iterator iter = criteriaEntityNames.values().iterator();
		while ( iter.hasNext() ) {
			String entityName = ( String ) iter.next();
			result.addAll( Arrays.asList( getFactory().getEntityPersister( entityName ).getQuerySpaces() ) );
		}
		return result;
	}

	private void createAliasCriteriaMap() {
		aliasCriteriaMap.put( rootCriteria.getAlias(), rootCriteria );
		Iterator iter = rootCriteria.iterateSubcriteria();
		while ( iter.hasNext() ) {
			Criteria subcriteria = ( Criteria ) iter.next();
			if ( subcriteria.getAlias() != null ) {
				Object old = aliasCriteriaMap.put( subcriteria.getAlias(), subcriteria );
				if ( old != null ) {
					throw new QueryException( "duplicate alias: " + subcriteria.getAlias() );
				}
			}
		}
	}

	private void createAssociationPathCriteriaMap() {
		Iterator iter = rootCriteria.iterateSubcriteria();
		while ( iter.hasNext() ) {
			CriteriaImpl.Subcriteria crit = ( CriteriaImpl.Subcriteria ) iter.next();
			String wholeAssociationPath = getWholeAssociationPath( crit );
			Object old = associationPathCriteriaMap.put( wholeAssociationPath, crit );
			if ( old != null ) {
				throw new QueryException( "duplicate association path: " + wholeAssociationPath );
			}
			int joinType = crit.getJoinType();
			old = associationPathJoinTypesMap.put( wholeAssociationPath, new Integer( joinType ) );
			if ( old != null ) {
				// TODO : not so sure this is needed...
				throw new QueryException( "duplicate association path: " + wholeAssociationPath );
			}
			if ( crit.getWithClause() != null )
			{
				this.withClauseMap.put(wholeAssociationPath, crit.getWithClause());
			}
		}
	}

	private String getWholeAssociationPath(CriteriaImpl.Subcriteria subcriteria) {
		String path = subcriteria.getPath();

		// some messy, complex stuff here, since createCriteria() can take an
		// aliased path, or a path rooted at the creating criteria instance
		Criteria parent = null;
		if ( path.indexOf( '.' ) > 0 ) {
			// if it is a compound path
			String testAlias = StringHelper.root( path );
			if ( !testAlias.equals( subcriteria.getAlias() ) ) {
				// and the qualifier is not the alias of this criteria
				//      -> check to see if we belong to some criteria other
				//          than the one that created us
				parent = ( Criteria ) aliasCriteriaMap.get( testAlias );
			}
		}
		if ( parent == null ) {
			// otherwise assume the parent is the the criteria that created us
			parent = subcriteria.getParent();
		}
		else {
			path = StringHelper.unroot( path );
		}

		if ( parent.equals( rootCriteria ) ) {
			// if its the root criteria, we are done
			return path;
		}
		else {
			// otherwise, recurse
			return getWholeAssociationPath( ( CriteriaImpl.Subcriteria ) parent ) + '.' + path;
		}
	}

	private void createCriteriaEntityNameMap() {
		criteriaEntityNames.put( rootCriteria, rootEntityName );
		Iterator iter = associationPathCriteriaMap.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = ( Map.Entry ) iter.next();
			criteriaEntityNames.put(
					me.getValue(), //the criteria instance
			        getPathEntityName( ( String ) me.getKey() )
			);
		}
	}

	private String getPathEntityName(String path) {
		Queryable persister = ( Queryable ) sessionFactory.getEntityPersister( rootEntityName );
		StringTokenizer tokens = new StringTokenizer( path, "." );
		String componentPath = "";
		while ( tokens.hasMoreTokens() ) {
			componentPath += tokens.nextToken();
			Type type = persister.toType( componentPath );
			if ( type.isAssociationType() ) {
				AssociationType atype = ( AssociationType ) type;
				persister = ( Queryable ) sessionFactory.getEntityPersister(
						atype.getAssociatedEntityName( sessionFactory )
				);
				componentPath = "";
			}
			else if ( type.isComponentType() ) {
				componentPath += '.';
			}
			else {
				throw new QueryException( "not an association: " + componentPath );
			}
		}
		return persister.getEntityName();
	}

	public int getSQLAliasCount() {
		return criteriaSQLAliasMap.size();
	}

	private void createCriteriaSQLAliasMap() {
		int i = 0;
		Iterator criteriaIterator = criteriaEntityNames.entrySet().iterator();
		while ( criteriaIterator.hasNext() ) {
			Map.Entry me = ( Map.Entry ) criteriaIterator.next();
			Criteria crit = ( Criteria ) me.getKey();
			String alias = crit.getAlias();
			if ( alias == null ) {
				alias = ( String ) me.getValue(); // the entity name
			}
			criteriaSQLAliasMap.put( crit, StringHelper.generateAlias( alias, i++ ) );
		}
		criteriaSQLAliasMap.put( rootCriteria, rootSQLAlias );
	}

	public CriteriaImpl getRootCriteria() {
		return rootCriteria;
	}

	public QueryParameters getQueryParameters() {
		LockOptions lockOptions = new LockOptions();
		RowSelection selection = new RowSelection();
		selection.setFirstRow( rootCriteria.getFirstResult() );
		selection.setMaxRows( rootCriteria.getMaxResults() );
		selection.setTimeout( rootCriteria.getTimeout() );
		selection.setFetchSize( rootCriteria.getFetchSize() );

		Iterator iter = rootCriteria.getLockModes().entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = ( Map.Entry ) iter.next();
			final Criteria subcriteria = getAliasedCriteria( ( String ) me.getKey() );
			lockOptions.setAliasSpecificLockMode( getSQLAlias( subcriteria ), (LockMode)me.getValue() );
		}
		List values = new ArrayList();
		List types = new ArrayList();
		iter = rootCriteria.iterateSubcriteria();
		while ( iter.hasNext() ) {
			CriteriaImpl.Subcriteria subcriteria = ( CriteriaImpl.Subcriteria ) iter.next();
			LockMode lm = subcriteria.getLockMode();
			if ( lm != null ) {
				lockOptions.setAliasSpecificLockMode( getSQLAlias( subcriteria ), lm );
			}
			if ( subcriteria.getWithClause() != null )
			{
				TypedValue[] tv = subcriteria.getWithClause().getTypedValues( subcriteria, this );
				for ( int i = 0; i < tv.length; i++ ) {
					values.add( tv[i].getValue() );
					types.add( tv[i].getType() );
				}
			}
		}

		// Type and value gathering for the WHERE clause needs to come AFTER lock mode gathering,
		// because the lock mode gathering loop now contains join clauses which can contain
		// parameter bindings (as in the HQL WITH clause).
		iter = rootCriteria.iterateExpressionEntries();
		while ( iter.hasNext() ) {
			CriteriaImpl.CriterionEntry ce = ( CriteriaImpl.CriterionEntry ) iter.next();
			TypedValue[] tv = ce.getCriterion().getTypedValues( ce.getCriteria(), this );
			for ( int i = 0; i < tv.length; i++ ) {
				values.add( tv[i].getValue() );
				types.add( tv[i].getType() );
			}
		}

		Object[] valueArray = values.toArray();
		Type[] typeArray = ArrayHelper.toTypeArray( types );
		return new QueryParameters(
				typeArray,
		        valueArray,
		        lockOptions,
		        selection,
		        rootCriteria.isReadOnlyInitialized(),
		        ( rootCriteria.isReadOnlyInitialized() ? rootCriteria.isReadOnly() : false ),
		        rootCriteria.getCacheable(),
		        rootCriteria.getCacheRegion(),
		        rootCriteria.getComment(),
		        rootCriteria.isLookupByNaturalKey(),
		        rootCriteria.getResultTransformer()
		);
	}

	public boolean hasProjection() {
		return rootCriteria.getProjection() != null;
	}

	public String getGroupBy() {
		if ( rootCriteria.getProjection().isGrouped() ) {
			return rootCriteria.getProjection()
					.toGroupSqlString( rootCriteria.getProjectionCriteria(), this );
		}
		else {
			return "";
		}
	}

	public String getSelect() {
		return rootCriteria.getProjection().toSqlString(
				rootCriteria.getProjectionCriteria(),
		        0,
		        this
		);
	}

	public Type[] getProjectedTypes() {
		return rootCriteria.getProjection().getTypes( rootCriteria, this );
	}

	public String[] getProjectedColumnAliases() {
		return rootCriteria.getProjection() instanceof EnhancedProjection ?
				( ( EnhancedProjection ) rootCriteria.getProjection() ).getColumnAliases( 0, rootCriteria, this ) :
				rootCriteria.getProjection().getColumnAliases( 0 );
	}

	public String[] getProjectedAliases() {
		return rootCriteria.getProjection().getAliases();
	}

	public String getWhereCondition() {
		StringBuffer condition = new StringBuffer( 30 );
		Iterator criterionIterator = rootCriteria.iterateExpressionEntries();
		while ( criterionIterator.hasNext() ) {
			CriteriaImpl.CriterionEntry entry = ( CriteriaImpl.CriterionEntry ) criterionIterator.next();
			String sqlString = entry.getCriterion().toSqlString( entry.getCriteria(), this );
			condition.append( sqlString );
			if ( criterionIterator.hasNext() ) {
				condition.append( " and " );
			}
		}
		return condition.toString();
	}

	public String getOrderBy() {
		StringBuffer orderBy = new StringBuffer( 30 );
		Iterator criterionIterator = rootCriteria.iterateOrderings();
		while ( criterionIterator.hasNext() ) {
			CriteriaImpl.OrderEntry oe = ( CriteriaImpl.OrderEntry ) criterionIterator.next();
			orderBy.append( oe.getOrder().toSqlString( oe.getCriteria(), this ) );
			if ( criterionIterator.hasNext() ) {
				orderBy.append( ", " );
			}
		}
		return orderBy.toString();
	}

	public SessionFactoryImplementor getFactory() {
		return sessionFactory;
	}

	public String getSQLAlias(Criteria criteria) {
		return ( String ) criteriaSQLAliasMap.get( criteria );
	}

	public String getEntityName(Criteria criteria) {
		return ( String ) criteriaEntityNames.get( criteria );
	}

	public String getColumn(Criteria criteria, String propertyName) {
		String[] cols = getColumns( propertyName, criteria );
		if ( cols.length != 1 ) {
			throw new QueryException( "property does not map to a single column: " + propertyName );
		}
		return cols[0];
	}

	/**
	 * Get the names of the columns constrained
	 * by this criterion.
	 */
	public String[] getColumnsUsingProjection(
			Criteria subcriteria,
	        String propertyName) throws HibernateException {

		//first look for a reference to a projection alias
		final Projection projection = rootCriteria.getProjection();
		String[] projectionColumns = null;
		if ( projection != null ) {
			projectionColumns = ( projection instanceof EnhancedProjection ?
					( ( EnhancedProjection ) projection ).getColumnAliases( propertyName, 0, rootCriteria, this ) :
					projection.getColumnAliases( propertyName, 0 )
			);
		}
		if ( projectionColumns == null ) {
			//it does not refer to an alias of a projection,
			//look for a property
			try {
				return getColumns( propertyName, subcriteria );
			}
			catch ( HibernateException he ) {
				//not found in inner query , try the outer query
				if ( outerQueryTranslator != null ) {
					return outerQueryTranslator.getColumnsUsingProjection( subcriteria, propertyName );
				}
				else {
					throw he;
				}
			}
		}
		else {
			//it refers to an alias of a projection
			return projectionColumns;
		}
	}

	public String[] getIdentifierColumns(Criteria subcriteria) {
		String[] idcols =
				( ( Loadable ) getPropertyMapping( getEntityName( subcriteria ) ) ).getIdentifierColumnNames();
		return StringHelper.qualify( getSQLAlias( subcriteria ), idcols );
	}

	public Type getIdentifierType(Criteria subcriteria) {
		return ( ( Loadable ) getPropertyMapping( getEntityName( subcriteria ) ) ).getIdentifierType();
	}

	public TypedValue getTypedIdentifierValue(Criteria subcriteria, Object value) {
		final Loadable loadable = ( Loadable ) getPropertyMapping( getEntityName( subcriteria ) );
		return new TypedValue(
				loadable.getIdentifierType(),
		        value,
		        EntityMode.POJO
		);
	}

	public String[] getColumns(
			String propertyName,
	        Criteria subcriteria) throws HibernateException {
		return getPropertyMapping( getEntityName( subcriteria, propertyName ) )
				.toColumns(
						getSQLAlias( subcriteria, propertyName ),
				        getPropertyName( propertyName )
				);
	}

	public Type getTypeUsingProjection(Criteria subcriteria, String propertyName)
			throws HibernateException {

		//first look for a reference to a projection alias
		final Projection projection = rootCriteria.getProjection();
		Type[] projectionTypes = projection == null ?
		                         null :
		                         projection.getTypes( propertyName, subcriteria, this );

		if ( projectionTypes == null ) {
			try {
				//it does not refer to an alias of a projection,
				//look for a property
				return getType( subcriteria, propertyName );
			}
			catch ( HibernateException he ) {
				//not found in inner query , try the outer query
				if ( outerQueryTranslator != null ) {
					return outerQueryTranslator.getType( subcriteria, propertyName );
				}
				else {
					throw he;
				}
			}
		}
		else {
			if ( projectionTypes.length != 1 ) {
				//should never happen, i think
				throw new QueryException( "not a single-length projection: " + propertyName );
			}
			return projectionTypes[0];
		}
	}

	public Type getType(Criteria subcriteria, String propertyName)
			throws HibernateException {
		return getPropertyMapping( getEntityName( subcriteria, propertyName ) )
				.toType( getPropertyName( propertyName ) );
	}

	/**
	 * Get the a typed value for the given property value.
	 */
	public TypedValue getTypedValue(Criteria subcriteria, String propertyName, Object value)
			throws HibernateException {
		// Detect discriminator values...
		if ( value instanceof Class ) {
			Class entityClass = ( Class ) value;
			Queryable q = SessionFactoryHelper.findQueryableUsingImports( sessionFactory, entityClass.getName() );
			if ( q != null ) {
				Type type = q.getDiscriminatorType();
				String stringValue = q.getDiscriminatorSQLValue();
				if (stringValue != null && stringValue.length() > 2
						&& stringValue.startsWith("'")
						&& stringValue.endsWith("'")) { // remove the single
														// quotes
					stringValue = stringValue.substring(1,
							stringValue.length() - 1);
				}
				
				// Convert the string value into the proper type.
				if ( type instanceof NullableType ) {
					NullableType nullableType = ( NullableType ) type;
					value = nullableType.fromStringValue( stringValue );
				}
				else {
					throw new QueryException( "Unsupported discriminator type " + type );
				}
				return new TypedValue(
						type,
				        value,
				        EntityMode.POJO
				);
			}
		}
		// Otherwise, this is an ordinary value.
		return new TypedValue(
				getTypeUsingProjection( subcriteria, propertyName ),
		        value,
		        EntityMode.POJO
		);
	}

	private PropertyMapping getPropertyMapping(String entityName)
			throws MappingException {
		return ( PropertyMapping ) sessionFactory.getEntityPersister( entityName );
	}

	//TODO: use these in methods above

	public String getEntityName(Criteria subcriteria, String propertyName) {
		if ( propertyName.indexOf( '.' ) > 0 ) {
			String root = StringHelper.root( propertyName );
			Criteria crit = getAliasedCriteria( root );
			if ( crit != null ) {
				return getEntityName( crit );
			}
		}
		return getEntityName( subcriteria );
	}

	public String getSQLAlias(Criteria criteria, String propertyName) {
		if ( propertyName.indexOf( '.' ) > 0 ) {
			String root = StringHelper.root( propertyName );
			Criteria subcriteria = getAliasedCriteria( root );
			if ( subcriteria != null ) {
				return getSQLAlias( subcriteria );
			}
		}
		return getSQLAlias( criteria );
	}

	public String getPropertyName(String propertyName) {
		if ( propertyName.indexOf( '.' ) > 0 ) {
			String root = StringHelper.root( propertyName );
			Criteria crit = getAliasedCriteria( root );
			if ( crit != null ) {
				return propertyName.substring( root.length() + 1 );
			}
		}
		return propertyName;
	}

	public String getWithClause(String path)
	{
		final Criterion crit = (Criterion)this.withClauseMap.get(path);
		return crit == null ? null : crit.toSqlString(getCriteria(path), this);
	}
	
}
