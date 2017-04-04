/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.criteria;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.EnhancedProjection;
import org.hibernate.criterion.Projection;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.internal.ast.util.SessionFactoryHelper;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.StringRepresentableType;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class CriteriaQueryTranslator implements CriteriaQuery {

	public static final String ROOT_SQL_ALIAS = Criteria.ROOT_ALIAS + '_';

	private CriteriaQuery outerQueryTranslator;

	private final CriteriaImpl rootCriteria;
	private final String rootEntityName;
	private final String rootSQLAlias;

	private final Map<Criteria, CriteriaInfoProvider> criteriaInfoMap = new LinkedHashMap<Criteria, CriteriaInfoProvider>();
	private final Map<String, CriteriaInfoProvider> nameCriteriaInfoMap = new LinkedHashMap<String, CriteriaInfoProvider>();
	private final Map<Criteria, String> criteriaSQLAliasMap = new HashMap<Criteria, String>();
	private final Map<String, Criteria> aliasCriteriaMap = new HashMap<String, Criteria>();
	private final Map<String, Criteria> associationPathCriteriaMap = new LinkedHashMap<String, Criteria>();
	private final Map<String, JoinType> associationPathJoinTypesMap = new LinkedHashMap<String, JoinType>();
	private final Map<String, Criterion> withClauseMap = new HashMap<String, Criterion>();

	private final SessionFactoryImplementor sessionFactory;
	private final SessionFactoryHelper helper;

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
		this.helper = new SessionFactoryHelper( factory );
		createAliasCriteriaMap();
		createAssociationPathCriteriaMap();
		createCriteriaEntityNameMap();
		createCriteriaSQLAliasMap();
	}

	@Override
	public String generateSQLAlias() {
		int aliasCount = 0;
		return StringHelper.generateAlias( Criteria.ROOT_ALIAS, aliasCount ) + '_';
	}

	public String getRootSQLALias() {
		return rootSQLAlias;
	}

	private Criteria getAliasedCriteria(String alias) {
		return aliasCriteriaMap.get( alias );
	}

	public boolean isJoin(String path) {
		return associationPathCriteriaMap.containsKey( path );
	}

	public JoinType getJoinType(String path) {
		JoinType result = associationPathJoinTypesMap.get( path );
		return ( result == null ? JoinType.INNER_JOIN : result );
	}

	public Criteria getCriteria(String path) {
		return associationPathCriteriaMap.get( path );
	}

	public Set<Serializable> getQuerySpaces() {
		Set<Serializable> result = new HashSet<Serializable>();
		for ( CriteriaInfoProvider info : criteriaInfoMap.values() ) {
			result.addAll( Arrays.asList( info.getSpaces() ) );
		}
		return result;
	}

	private void createAliasCriteriaMap() {
		aliasCriteriaMap.put( rootCriteria.getAlias(), rootCriteria );
		Iterator<CriteriaImpl.Subcriteria> iter = rootCriteria.iterateSubcriteria();
		while ( iter.hasNext() ) {
			Criteria subcriteria = iter.next();
			if ( subcriteria.getAlias() != null ) {
				Object old = aliasCriteriaMap.put( subcriteria.getAlias(), subcriteria );
				if ( old != null ) {
					throw new QueryException( "duplicate alias: " + subcriteria.getAlias() );
				}
			}
		}
	}

	private void createAssociationPathCriteriaMap() {
		final Iterator<CriteriaImpl.Subcriteria> iter = rootCriteria.iterateSubcriteria();
		while ( iter.hasNext() ) {
			CriteriaImpl.Subcriteria crit = iter.next();
			String wholeAssociationPath = getWholeAssociationPath( crit );
			Object old = associationPathCriteriaMap.put( wholeAssociationPath, crit );
			if ( old != null ) {
				throw new QueryException( "duplicate association path: " + wholeAssociationPath );
			}
			JoinType joinType = crit.getJoinType();
			old = associationPathJoinTypesMap.put( wholeAssociationPath, joinType );
			if ( old != null ) {
				// TODO : not so sure this is needed...
				throw new QueryException( "duplicate association path: " + wholeAssociationPath );
			}
			if ( crit.getWithClause() != null ) {
				this.withClauseMap.put( wholeAssociationPath, crit.getWithClause() );
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
				parent = aliasCriteriaMap.get( testAlias );
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
			return getWholeAssociationPath( (CriteriaImpl.Subcriteria) parent ) + '.' + path;
		}
	}

	private void createCriteriaEntityNameMap() {
		// initialize the rootProvider first
		final CriteriaInfoProvider rootProvider = new EntityCriteriaInfoProvider(
				(Queryable) sessionFactory.getEntityPersister( rootEntityName )
		);
		criteriaInfoMap.put( rootCriteria, rootProvider );
		nameCriteriaInfoMap.put( rootProvider.getName(), rootProvider );

		for ( final String key : associationPathCriteriaMap.keySet() ) {
			final Criteria value = associationPathCriteriaMap.get( key );
			final CriteriaInfoProvider info = getPathInfo( key );
			criteriaInfoMap.put( value, info );
			nameCriteriaInfoMap.put( info.getName(), info );
		}
	}


	private CriteriaInfoProvider getPathInfo(String path) {
		StringTokenizer tokens = new StringTokenizer( path, "." );
		String componentPath = "";

		// start with the 'rootProvider'
		CriteriaInfoProvider provider = nameCriteriaInfoMap.get( rootEntityName );

		while ( tokens.hasMoreTokens() ) {
			componentPath += tokens.nextToken();
			final Type type = provider.getType( componentPath );
			if ( type.isAssociationType() ) {
				// CollectionTypes are always also AssociationTypes - but there's not always an associated entity...
				final AssociationType atype = (AssociationType) type;
				final CollectionType ctype = type.isCollectionType() ? (CollectionType) type : null;
				final Type elementType = ( ctype != null ) ? ctype.getElementType( sessionFactory ) : null;
				// is the association a collection of components or value-types? (i.e a colloction of valued types?)
				if ( ctype != null && elementType.isComponentType() ) {
					provider = new ComponentCollectionCriteriaInfoProvider( helper.getCollectionPersister( ctype.getRole() ) );
				}
				else if ( ctype != null && !elementType.isEntityType() ) {
					provider = new ScalarCollectionCriteriaInfoProvider( helper, ctype.getRole() );
				}
				else {
					provider = new EntityCriteriaInfoProvider(
							(Queryable) sessionFactory.getEntityPersister( atype.getAssociatedEntityName( sessionFactory ) )
					);
				}

				componentPath = "";
			}
			else if ( type.isComponentType() ) {
				if ( !tokens.hasMoreTokens() ) {
					throw new QueryException(
							"Criteria objects cannot be created directly on components.  Create a criteria on " +
									"owning entity and use a dotted property to access component property: " + path
					);
				}
				else {
					componentPath += '.';
				}
			}
			else {
				throw new QueryException( "not an association: " + componentPath );
			}
		}

		return provider;
	}

	public int getSQLAliasCount() {
		return criteriaSQLAliasMap.size();
	}

	private void createCriteriaSQLAliasMap() {
		int i = 0;
		for ( final Criteria crit : criteriaInfoMap.keySet() ) {
			final CriteriaInfoProvider value = criteriaInfoMap.get( crit );
			String alias = crit.getAlias();
			if ( alias == null ) {
				// the entity name
				alias = value.getName();
			}
			criteriaSQLAliasMap.put( crit, StringHelper.generateAlias( alias, i++ ) );
		}

		criteriaSQLAliasMap.put( rootCriteria, rootSQLAlias );
	}

	public CriteriaImpl getRootCriteria() {
		return rootCriteria;
	}

	public QueryParameters getQueryParameters() {
		final RowSelection selection = new RowSelection();
		selection.setFirstRow( rootCriteria.getFirstResult() );
		selection.setMaxRows( rootCriteria.getMaxResults() );
		selection.setTimeout( rootCriteria.getTimeout() );
		selection.setFetchSize( rootCriteria.getFetchSize() );

		final LockOptions lockOptions = new LockOptions();
		final Map<String, LockMode> lockModeMap = rootCriteria.getLockModes();
		for ( final String key : lockModeMap.keySet() ) {
			final Criteria subcriteria = getAliasedCriteria( key );
			lockOptions.setAliasSpecificLockMode( getSQLAlias( subcriteria ), lockModeMap.get( key ) );
		}

		final List<Object> values = new ArrayList<Object>();
		final List<Type> types = new ArrayList<Type>();
		final Iterator<CriteriaImpl.Subcriteria> subcriteriaIterator = rootCriteria.iterateSubcriteria();
		while ( subcriteriaIterator.hasNext() ) {
			final CriteriaImpl.Subcriteria subcriteria = subcriteriaIterator.next();
			final LockMode lm = subcriteria.getLockMode();
			if ( lm != null ) {
				lockOptions.setAliasSpecificLockMode( getSQLAlias( subcriteria ), lm );
			}
			if ( subcriteria.getWithClause() != null ) {
				final TypedValue[] tv = subcriteria.getWithClause().getTypedValues( subcriteria, this );
				for ( TypedValue aTv : tv ) {
					values.add( aTv.getValue() );
					types.add( aTv.getType() );
				}
			}
		}

		// Type and value gathering for the WHERE clause needs to come AFTER lock mode gathering,
		// because the lock mode gathering loop now contains join clauses which can contain
		// parameter bindings (as in the HQL WITH clause).
		final Iterator<CriteriaImpl.CriterionEntry> iter = rootCriteria.iterateExpressionEntries();
		while ( iter.hasNext() ) {
			final CriteriaImpl.CriterionEntry ce = iter.next();
			final TypedValue[] tv = ce.getCriterion().getTypedValues( ce.getCriteria(), this );
			for ( TypedValue aTv : tv ) {
				values.add( aTv.getValue() );
				types.add( aTv.getType() );
			}
		}

		final Object[] valueArray = values.toArray();
		final Type[] typeArray = ArrayHelper.toTypeArray( types );
		return new QueryParameters(
				typeArray,
				valueArray,
				lockOptions,
				selection,
				rootCriteria.isReadOnlyInitialized(),
				( rootCriteria.isReadOnlyInitialized() && rootCriteria.isReadOnly() ),
				rootCriteria.getCacheable(),
				rootCriteria.getCacheRegion(),
				rootCriteria.getComment(),
				rootCriteria.getQueryHints(),
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

	/* package-protected */
	Type getResultType(Criteria criteria) {
		return getFactory().getTypeResolver().getTypeFactory().manyToOne( getEntityName( criteria ) );
	}

	public Type[] getProjectedTypes() {
		return rootCriteria.getProjection().getTypes( rootCriteria, this );
	}

	public String[] getProjectedColumnAliases() {
		return rootCriteria.getProjection() instanceof EnhancedProjection ?
				( (EnhancedProjection) rootCriteria.getProjection() ).getColumnAliases( 0, rootCriteria, this ) :
				rootCriteria.getProjection().getColumnAliases( 0 );
	}

	public String[] getProjectedAliases() {
		return rootCriteria.getProjection().getAliases();
	}

	public String getWhereCondition() {
		StringBuilder condition = new StringBuilder( 30 );
		Iterator<CriteriaImpl.CriterionEntry> criterionIterator = rootCriteria.iterateExpressionEntries();
		while ( criterionIterator.hasNext() ) {
			CriteriaImpl.CriterionEntry entry = criterionIterator.next();
			String sqlString = entry.getCriterion().toSqlString( entry.getCriteria(), this );
			condition.append( sqlString );
			if ( criterionIterator.hasNext() ) {
				condition.append( " and " );
			}
		}
		return condition.toString();
	}

	public String getOrderBy() {
		StringBuilder orderBy = new StringBuilder( 30 );
		Iterator<CriteriaImpl.OrderEntry> criterionIterator = rootCriteria.iterateOrderings();
		while ( criterionIterator.hasNext() ) {
			CriteriaImpl.OrderEntry oe = criterionIterator.next();
			orderBy.append( oe.getOrder().toSqlString( oe.getCriteria(), this ) );
			if ( criterionIterator.hasNext() ) {
				orderBy.append( ", " );
			}
		}
		return orderBy.toString();
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return sessionFactory;
	}

	@Override
	public String getSQLAlias(Criteria criteria) {
		return criteriaSQLAliasMap.get( criteria );
	}

	@Override
	public String getEntityName(Criteria criteria) {
		final CriteriaInfoProvider infoProvider = criteriaInfoMap.get( criteria );
		return infoProvider != null ? infoProvider.getName() : null;
	}

	@Override
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
	@Override
	public String[] getColumnsUsingProjection(
			Criteria subcriteria,
			String propertyName) throws HibernateException {

		//first look for a reference to a projection alias
		final Projection projection = rootCriteria.getProjection();
		String[] projectionColumns = null;
		if ( projection != null ) {
			projectionColumns = ( projection instanceof EnhancedProjection ?
					( (EnhancedProjection) projection ).getColumnAliases( propertyName, 0, rootCriteria, this ) :
					projection.getColumnAliases( propertyName, 0 )
			);
		}
		if ( projectionColumns == null ) {
			//it does not refer to an alias of a projection,
			//look for a property
			try {
				return getColumns( propertyName, subcriteria );
			}
			catch (HibernateException he) {
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

	@Override
	public String[] getIdentifierColumns(Criteria criteria) {
		String[] idcols =
				( (Loadable) getPropertyMapping( getEntityName( criteria ) ) ).getIdentifierColumnNames();
		return StringHelper.qualify( getSQLAlias( criteria ), idcols );
	}

	@Override
	public Type getIdentifierType(Criteria criteria) {
		return ( (Loadable) getPropertyMapping( getEntityName( criteria ) ) ).getIdentifierType();
	}

	@Override
	public TypedValue getTypedIdentifierValue(Criteria criteria, Object value) {
		final Loadable loadable = (Loadable) getPropertyMapping( getEntityName( criteria ) );
		return new TypedValue( loadable.getIdentifierType(), value );
	}

	@Override
	public String[] getColumns(
			String propertyName,
			Criteria subcriteria) throws HibernateException {
		return getPropertyMapping( getEntityName( subcriteria, propertyName ) )
				.toColumns(
						getSQLAlias( subcriteria, propertyName ),
						getPropertyName( propertyName )
				);
	}

	/**
	 * Get the names of the columns mapped by a property path; if the
	 * property path is not found in subcriteria, try the "outer" query.
	 * Projection aliases are ignored.
	 */
	@Override
	public String[] findColumns(String propertyName, Criteria subcriteria)
			throws HibernateException {
		try {
			return getColumns( propertyName, subcriteria );
		}
		catch (HibernateException he) {
			//not found in inner query, try the outer query
			if ( outerQueryTranslator != null ) {
				return outerQueryTranslator.findColumns( propertyName, subcriteria );
			}
			else {
				throw he;
			}
		}
	}

	@Override
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
			catch (HibernateException he) {
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

	@Override
	public Type getType(Criteria subcriteria, String propertyName)
			throws HibernateException {
		return getPropertyMapping( getEntityName( subcriteria, propertyName ) )
				.toType( getPropertyName( propertyName ) );
	}

	/**
	 * Get the a typed value for the given property value.
	 */
	@Override
	public TypedValue getTypedValue(Criteria subcriteria, String propertyName, Object value) throws HibernateException {
		// Detect discriminator values...
		if ( value instanceof Class ) {
			final Class entityClass = (Class) value;
			final Queryable q = SessionFactoryHelper.findQueryableUsingImports( sessionFactory, entityClass.getName() );
			if ( q != null ) {
				final Type type = q.getDiscriminatorType();
				String stringValue = q.getDiscriminatorSQLValue();
				if ( stringValue != null
						&& stringValue.length() > 2
						&& stringValue.startsWith( "'" )
						&& stringValue.endsWith( "'" ) ) {
					// remove the single quotes
					stringValue = stringValue.substring( 1, stringValue.length() - 1 );
				}

				// Convert the string value into the proper type.
				if ( type instanceof StringRepresentableType ) {
					final StringRepresentableType nullableType = (StringRepresentableType) type;
					value = nullableType.fromStringValue( stringValue );
				}
				else {
					throw new QueryException( "Unsupported discriminator type " + type );
				}
				return new TypedValue( type, value );
			}
		}
		// Otherwise, this is an ordinary value.
		return new TypedValue( getTypeUsingProjection( subcriteria, propertyName ), value );
	}

	private PropertyMapping getPropertyMapping(String entityName) throws MappingException {
		final CriteriaInfoProvider info = nameCriteriaInfoMap.get( entityName );
		if ( info == null ) {
			throw new HibernateException( "Unknown entity: " + entityName );
		}
		return info.getPropertyMapping();
	}

	//TODO: use these in methods above
	@Override
	public String getEntityName(Criteria subcriteria, String propertyName) {
		if ( propertyName.indexOf( '.' ) > 0 ) {
			final String root = StringHelper.root( propertyName );
			final Criteria crit = getAliasedCriteria( root );
			if ( crit != null ) {
				return getEntityName( crit );
			}
		}
		return getEntityName( subcriteria );
	}

	@Override
	public String getSQLAlias(Criteria criteria, String propertyName) {
		if ( propertyName.indexOf( '.' ) > 0 ) {
			final String root = StringHelper.root( propertyName );
			final Criteria subcriteria = getAliasedCriteria( root );
			if ( subcriteria != null ) {
				return getSQLAlias( subcriteria );
			}
		}
		return getSQLAlias( criteria );
	}

	@Override
	public String getPropertyName(String propertyName) {
		if ( propertyName.indexOf( '.' ) > 0 ) {
			final String root = StringHelper.root( propertyName );
			final Criteria criteria = getAliasedCriteria( root );
			if ( criteria != null ) {
				return propertyName.substring( root.length() + 1 );
			}
		}
		return propertyName;
	}

	public String getWithClause(String path) {
		final Criterion criterion = withClauseMap.get( path );
		return criterion == null ? null : criterion.toSqlString( getCriteria( path ), this );
	}

	public boolean hasRestriction(String path) {
		final CriteriaImpl.Subcriteria subcriteria = (CriteriaImpl.Subcriteria) getCriteria( path );
		return subcriteria != null && subcriteria.hasRestriction();
	}

}
