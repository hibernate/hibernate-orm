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
package org.hibernate.hql.internal.classic;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.ParameterTranslations;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.IteratorImpl;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.QuerySelect;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * An instance of <tt>QueryTranslator</tt> translates a Hibernate
 * query string to SQL.
 */
public class QueryTranslatorImpl extends BasicLoader implements FilterTranslator {
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueryTranslatorImpl.class );

	private static final String[] NO_RETURN_ALIASES = new String[] {};

	private final String queryIdentifier;
	private final String queryString;

	private final Map typeMap = new LinkedHashMap();
	private final Map collections = new LinkedHashMap();
	private List returnedTypes = new ArrayList();
	private final List fromTypes = new ArrayList();
	private final List scalarTypes = new ArrayList();
	private final Map namedParameters = new HashMap();
	private final Map aliasNames = new HashMap();
	private final Map oneToOneOwnerNames = new HashMap();
	private final Map uniqueKeyOwnerReferences = new HashMap();
	private final Map decoratedPropertyMappings = new HashMap();

	private final List scalarSelectTokens = new ArrayList();
	private final List whereTokens = new ArrayList();
	private final List havingTokens = new ArrayList();
	private final Map joins = new LinkedHashMap();
	private final List orderByTokens = new ArrayList();
	private final List groupByTokens = new ArrayList();
	private final Set<Serializable> querySpaces = new HashSet<Serializable>();
	private final Set entitiesToFetch = new HashSet();

	private final Map pathAliases = new HashMap();
	private final Map pathJoins = new HashMap();

	private Queryable[] persisters;
	private int[] owners;
	private EntityType[] ownerAssociationTypes;
	private String[] names;
	private boolean[] includeInSelect;
	private int selectLength;
	private Type[] returnTypes;
	private Type[] actualReturnTypes;
	private String[][] scalarColumnNames;
	private Map tokenReplacements;
	private int nameCount;
	private int parameterCount;
	private boolean distinct;
	private boolean compiled;
	private String sqlString;
	private Class holderClass;
	private Constructor holderConstructor;
	private boolean hasScalars;
	private boolean shallowQuery;
	private QueryTranslatorImpl superQuery;

	private QueryableCollection collectionPersister;
	private int collectionOwnerColumn = -1;
	private String collectionOwnerName;
	private String fetchName;

	private String[] suffixes;

	private Map enabledFilters;

	/**
	 * Construct a query translator
	 *
	 * @param queryIdentifier A unique identifier for the query of which this
	 * translation is part; typically this is the original, user-supplied query string.
	 * @param queryString The "preprocessed" query string; at the very least
	 * already processed by {@link org.hibernate.hql.internal.QuerySplitter}.
	 * @param enabledFilters Any enabled filters.
	 * @param factory The session factory.
	 */
	public QueryTranslatorImpl(
			String queryIdentifier,
	        String queryString,
	        Map enabledFilters,
	        SessionFactoryImplementor factory) {
		super( factory );
		this.queryIdentifier = queryIdentifier;
		this.queryString = queryString;
		this.enabledFilters = enabledFilters;
	}

	/**
	 * Construct a query translator; this form used internally.
	 *
	 * @param queryString The query string to process.
	 * @param enabledFilters Any enabled filters.
	 * @param factory The session factory.
	 */
	public QueryTranslatorImpl(
	        String queryString,
	        Map enabledFilters,
	        SessionFactoryImplementor factory) {
		this( queryString, queryString, enabledFilters, factory );
	}

	/**
	 * Compile a subquery.
	 *
	 * @param superquery The containing query of the query to be compiled.
	 *
	 * @throws org.hibernate.MappingException Indicates problems resolving
	 * things referenced in the query.
	 * @throws org.hibernate.QueryException Generally some form of syntatic
	 * failure.
	 */
	void compile(QueryTranslatorImpl superquery) throws QueryException, MappingException {
		this.tokenReplacements = superquery.tokenReplacements;
		this.superQuery = superquery;
		this.shallowQuery = true;
		this.enabledFilters = superquery.getEnabledFilters();
		compile();
	}


	/**
	 * Compile a "normal" query. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 */
	public synchronized void compile(
			Map replacements,
			boolean scalar) throws QueryException, MappingException {
		if ( !compiled ) {
			this.tokenReplacements = replacements;
			this.shallowQuery = scalar;
			compile();
		}
	}

	/**
	 * Compile a filter. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 */
	public synchronized void compile(
			String collectionRole,
			Map replacements,
			boolean scalar) throws QueryException, MappingException {

		if ( !isCompiled() ) {
			addFromAssociation( "this", collectionRole );
			compile( replacements, scalar );
		}
	}

	/**
	 * Compile the query (generate the SQL).
	 *
	 * @throws org.hibernate.MappingException Indicates problems resolving
	 * things referenced in the query.
	 * @throws org.hibernate.QueryException Generally some form of syntatic
	 * failure.
	 */
	private void compile() throws QueryException, MappingException {
		LOG.trace( "Compiling query" );
		try {
			ParserHelper.parse( new PreprocessingParser( tokenReplacements ),
					queryString,
					ParserHelper.HQL_SEPARATORS,
					this );
			renderSQL();
		}
		catch ( QueryException qe ) {
			if ( qe.getQueryString() == null ) {
				throw qe.wrapWithQueryString( queryString );
			}
			else {
				throw qe;
			}
		}
		catch ( MappingException me ) {
			throw me;
		}
		catch ( Exception e ) {
			LOG.debug( "Unexpected query compilation problem", e );
			e.printStackTrace();
			throw new QueryException( "Incorrect query syntax", queryString, e );
		}

		postInstantiate();

		compiled = true;

	}

	@Override
    public String getSQLString() {
		return sqlString;
	}

	public List<String> collectSqlStrings() {
		return ArrayHelper.toList( new String[] { sqlString } );
	}

	public String getQueryString() {
		return queryString;
	}

	/**
	 * Persisters for the return values of a <tt>find()</tt> style query.
	 *
	 * @return an array of <tt>EntityPersister</tt>s.
	 */
	@Override
    protected Loadable[] getEntityPersisters() {
		return persisters;
	}

	/**
	 * Types of the return values of an <tt>iterate()</tt> style query.
	 *
	 * @return an array of <tt>Type</tt>s.
	 */
	public Type[] getReturnTypes() {
		return actualReturnTypes;
	}

	public String[] getReturnAliases() {
		// return aliases not supported in classic translator!
		return NO_RETURN_ALIASES;
	}

	public String[][] getColumnNames() {
		return scalarColumnNames;
	}

	private static void logQuery(String hql, String sql) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "HQL: %s", hql );
			LOG.debugf( "SQL: %s", sql );
		}
	}

	void setAliasName(String alias, String name) {
		aliasNames.put( alias, name );
	}

	public String getAliasName(String alias) {
		String name = ( String ) aliasNames.get( alias );
		if ( name == null ) {
			if ( superQuery != null ) {
				name = superQuery.getAliasName( alias );
			}
			else {
				name = alias;
			}
		}
		return name;
	}

	String unalias(String path) {
		String alias = StringHelper.root( path );
		String name = getAliasName( alias );
        if (name != null) return name + path.substring(alias.length());
        return path;
	}

	void addEntityToFetch(String name, String oneToOneOwnerName, AssociationType ownerAssociationType) {
		addEntityToFetch( name );
		if ( oneToOneOwnerName != null ) oneToOneOwnerNames.put( name, oneToOneOwnerName );
		if ( ownerAssociationType != null ) uniqueKeyOwnerReferences.put( name, ownerAssociationType );
	}

	private void addEntityToFetch(String name) {
		entitiesToFetch.add( name );
	}

	private int nextCount() {
		return ( superQuery == null ) ? nameCount++ : superQuery.nameCount++;
	}

	String createNameFor(String type) {
		return StringHelper.generateAlias( type, nextCount() );
	}

	String createNameForCollection(String role) {
		return StringHelper.generateAlias( role, nextCount() );
	}

	private String getType(String name) {
		String type = ( String ) typeMap.get( name );
		if ( type == null && superQuery != null ) {
			type = superQuery.getType( name );
		}
		return type;
	}

	private String getRole(String name) {
		String role = ( String ) collections.get( name );
		if ( role == null && superQuery != null ) {
			role = superQuery.getRole( name );
		}
		return role;
	}

	boolean isName(String name) {
		return aliasNames.containsKey( name ) ||
				typeMap.containsKey( name ) ||
				collections.containsKey( name ) || (
				superQuery != null && superQuery.isName( name )
				);
	}

	PropertyMapping getPropertyMapping(String name) throws QueryException {
		PropertyMapping decorator = getDecoratedPropertyMapping( name );
		if ( decorator != null ) return decorator;

		String type = getType( name );
		if ( type == null ) {
			String role = getRole( name );
			if ( role == null ) {
				throw new QueryException( "alias not found: " + name );
			}
			return getCollectionPersister( role ); //.getElementPropertyMapping();
		}
		else {
			Queryable persister = getEntityPersister( type );
			if ( persister == null ) throw new QueryException( "persistent class not found: " + type );
			return persister;
		}
	}

	private PropertyMapping getDecoratedPropertyMapping(String name) {
		return ( PropertyMapping ) decoratedPropertyMappings.get( name );
	}

	void decoratePropertyMapping(String name, PropertyMapping mapping) {
		decoratedPropertyMappings.put( name, mapping );
	}

	private Queryable getEntityPersisterForName(String name) throws QueryException {
		String type = getType( name );
		Queryable persister = getEntityPersister( type );
		if ( persister == null ) throw new QueryException( "persistent class not found: " + type );
		return persister;
	}

	Queryable getEntityPersisterUsingImports(String className) {
		final String importedClassName = getFactory().getImportedClassName( className );
		if ( importedClassName == null ) {
			return null;
		}
		try {
			return ( Queryable ) getFactory().getEntityPersister( importedClassName );
		}
		catch ( MappingException me ) {
			return null;
		}
	}

	Queryable getEntityPersister(String entityName) throws QueryException {
		try {
			return ( Queryable ) getFactory().getEntityPersister( entityName );
		}
		catch ( Exception e ) {
			throw new QueryException( "persistent class not found: " + entityName );
		}
	}

	QueryableCollection getCollectionPersister(String role) throws QueryException {
		try {
			return ( QueryableCollection ) getFactory().getCollectionPersister( role );
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection role is not queryable: " + role );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection role not found: " + role );
		}
	}

	void addType(String name, String type) {
		typeMap.put( name, type );
	}

	void addCollection(String name, String role) {
		collections.put( name, role );
	}

	void addFrom(String name, String type, JoinSequence joinSequence)
			throws QueryException {
		addType( name, type );
		addFrom( name, joinSequence );
	}

	void addFromCollection(String name, String collectionRole, JoinSequence joinSequence)
			throws QueryException {
		//register collection role
		addCollection( name, collectionRole );
		addJoin( name, joinSequence );
	}

	void addFrom(String name, JoinSequence joinSequence)
			throws QueryException {
		fromTypes.add( name );
		addJoin( name, joinSequence );
	}

	void addFromClass(String name, Queryable classPersister)
			throws QueryException {
		JoinSequence joinSequence = new JoinSequence( getFactory() )
				.setRoot( classPersister, name );
		//crossJoins.add(name);
		addFrom( name, classPersister.getEntityName(), joinSequence );
	}

	void addSelectClass(String name) {
		returnedTypes.add( name );
	}

	void addSelectScalar(Type type) {
		scalarTypes.add( type );
	}

	void appendWhereToken(String token) {
		whereTokens.add( token );
	}

	void appendHavingToken(String token) {
		havingTokens.add( token );
	}

	void appendOrderByToken(String token) {
		orderByTokens.add( token );
	}

	void appendGroupByToken(String token) {
		groupByTokens.add( token );
	}

	void appendScalarSelectToken(String token) {
		scalarSelectTokens.add( token );
	}

	void appendScalarSelectTokens(String[] tokens) {
		scalarSelectTokens.add( tokens );
	}

	void addFromJoinOnly(String name, JoinSequence joinSequence) throws QueryException {
		addJoin( name, joinSequence.getFromPart() );
	}

	void addJoin(String name, JoinSequence joinSequence) throws QueryException {
		if ( !joins.containsKey( name ) ) joins.put( name, joinSequence );
	}

	void addNamedParameter(String name) {
		if ( superQuery != null ) superQuery.addNamedParameter( name );
		Integer loc = parameterCount++;
		Object o = namedParameters.get( name );
		if ( o == null ) {
			namedParameters.put( name, loc );
		}
		else if ( o instanceof Integer ) {
			ArrayList list = new ArrayList( 4 );
			list.add( o );
			list.add( loc );
			namedParameters.put( name, list );
		}
		else {
			( ( ArrayList ) o ).add( loc );
		}
	}

	@Override
    public int[] getNamedParameterLocs(String name) throws QueryException {
		Object o = namedParameters.get( name );
		if ( o == null ) {
			throw new QueryException( ERROR_NAMED_PARAMETER_DOES_NOT_APPEAR + name, queryString );
		}
		if ( o instanceof Integer ) return new int[] { (Integer) o };
		else {
			return ArrayHelper.toIntArray( ( ArrayList ) o );
		}
	}

	private void renderSQL() throws QueryException, MappingException {

		final int rtsize;
		if ( returnedTypes.size() == 0 && scalarTypes.size() == 0 ) {
			//ie no select clause in HQL
			returnedTypes = fromTypes;
			rtsize = returnedTypes.size();
		}
		else {
			rtsize = returnedTypes.size();
			Iterator iter = entitiesToFetch.iterator();
			while ( iter.hasNext() ) {
				returnedTypes.add( iter.next() );
			}
		}
		int size = returnedTypes.size();
		persisters = new Queryable[size];
		names = new String[size];
		owners = new int[size];
		ownerAssociationTypes = new EntityType[size];
		suffixes = new String[size];
		includeInSelect = new boolean[size];
		for ( int i = 0; i < size; i++ ) {
			String name = ( String ) returnedTypes.get( i );
			//if ( !isName(name) ) throw new QueryException("unknown type: " + name);
			persisters[i] = getEntityPersisterForName( name );
			// TODO: cannot use generateSuffixes() - it handles the initial suffix differently.
			suffixes[i] = ( size == 1 ) ? "" : Integer.toString( i ) + '_';
			names[i] = name;
			includeInSelect[i] = !entitiesToFetch.contains( name );
			if ( includeInSelect[i] ) selectLength++;
			if ( name.equals( collectionOwnerName ) ) collectionOwnerColumn = i;
			String oneToOneOwner = ( String ) oneToOneOwnerNames.get( name );
			owners[i] = ( oneToOneOwner == null ) ? -1 : returnedTypes.indexOf( oneToOneOwner );
			ownerAssociationTypes[i] = (EntityType) uniqueKeyOwnerReferences.get( name );
		}

		if ( ArrayHelper.isAllNegative( owners ) ) owners = null;

		String scalarSelect = renderScalarSelect(); //Must be done here because of side-effect! yuck...

		int scalarSize = scalarTypes.size();
		hasScalars = scalarTypes.size() != rtsize;

		returnTypes = new Type[scalarSize];
		for ( int i = 0; i < scalarSize; i++ ) {
			returnTypes[i] = ( Type ) scalarTypes.get( i );
		}

		QuerySelect sql = new QuerySelect( getFactory().getDialect() );
		sql.setDistinct( distinct );

		if ( !shallowQuery ) {
			renderIdentifierSelect( sql );
			renderPropertiesSelect( sql );
		}

		if ( collectionPersister != null ) {
			sql.addSelectFragmentString( collectionPersister.selectFragment( fetchName, "__" ) );
		}

		if ( hasScalars || shallowQuery ) sql.addSelectFragmentString( scalarSelect );

		//TODO: for some dialects it would be appropriate to add the renderOrderByPropertiesSelect() to other select strings
		mergeJoins( sql.getJoinFragment() );

		sql.setWhereTokens( whereTokens.iterator() );

		sql.setGroupByTokens( groupByTokens.iterator() );
		sql.setHavingTokens( havingTokens.iterator() );
		sql.setOrderByTokens( orderByTokens.iterator() );

		if ( collectionPersister != null && collectionPersister.hasOrdering() ) {
			sql.addOrderBy( collectionPersister.getSQLOrderByString( fetchName ) );
		}

		scalarColumnNames = NameGenerator.generateColumnNames( returnTypes, getFactory() );

		// initialize the Set of queried identifier spaces (ie. tables)
		Iterator iter = collections.values().iterator();
		while ( iter.hasNext() ) {
			CollectionPersister p = getCollectionPersister( ( String ) iter.next() );
			addQuerySpaces( p.getCollectionSpaces() );
		}
		iter = typeMap.keySet().iterator();
		while ( iter.hasNext() ) {
			Queryable p = getEntityPersisterForName( ( String ) iter.next() );
			addQuerySpaces( p.getQuerySpaces() );
		}

		sqlString = sql.toQueryString();

		if ( holderClass != null ) holderConstructor = ReflectHelper.getConstructor( holderClass, returnTypes );

		if ( hasScalars ) {
			actualReturnTypes = returnTypes;
		}
		else {
			actualReturnTypes = new Type[selectLength];
			int j = 0;
			for ( int i = 0; i < persisters.length; i++ ) {
				if ( includeInSelect[i] ) {
					actualReturnTypes[j++] = getFactory().getTypeResolver()
							.getTypeFactory()
							.manyToOne( persisters[i].getEntityName(), shallowQuery );
				}
			}
		}

	}

	private void renderIdentifierSelect(QuerySelect sql) {
		int size = returnedTypes.size();

		for ( int k = 0; k < size; k++ ) {
			String name = ( String ) returnedTypes.get( k );
			String suffix = size == 1 ? "" : Integer.toString( k ) + '_';
			sql.addSelectFragmentString( persisters[k].identifierSelectFragment( name, suffix ) );
		}

	}

	/*private String renderOrderByPropertiesSelect() {
		StringBuffer buf = new StringBuffer(10);

		//add the columns we are ordering by to the select ID select clause
		Iterator iter = orderByTokens.iterator();
		while ( iter.hasNext() ) {
			String token = (String) iter.next();
			if ( token.lastIndexOf(".") > 0 ) {
				//ie. it is of form "foo.bar", not of form "asc" or "desc"
				buf.append(StringHelper.COMMA_SPACE).append(token);
			}
		}

		return buf.toString();
	}*/

	private void renderPropertiesSelect(QuerySelect sql) {
		int size = returnedTypes.size();
		for ( int k = 0; k < size; k++ ) {
			String suffix = size == 1 ? "" : Integer.toString( k ) + '_';
			String name = ( String ) returnedTypes.get( k );
			sql.addSelectFragmentString( persisters[k].propertySelectFragment( name, suffix, false ) );
		}
	}

	/**
	 * WARNING: side-effecty
	 */
	private String renderScalarSelect() {

		boolean isSubselect = superQuery != null;

		StringBuilder buf = new StringBuilder( 20 );

		if ( scalarTypes.size() == 0 ) {
			//ie. no select clause
			int size = returnedTypes.size();
			for ( int k = 0; k < size; k++ ) {

				scalarTypes.add(
						getFactory().getTypeResolver().getTypeFactory().manyToOne( persisters[k].getEntityName(), shallowQuery )
				);

				String[] idColumnNames = persisters[k].getIdentifierColumnNames();
				for ( int i = 0; i < idColumnNames.length; i++ ) {
					buf.append( returnedTypes.get( k ) ).append( '.' ).append( idColumnNames[i] );
					if ( !isSubselect ) buf.append( " as " ).append( NameGenerator.scalarName( k, i ) );
					if ( i != idColumnNames.length - 1 || k != size - 1 ) buf.append( ", " );
				}

			}

		}
		else {
			//there _was_ a select clause
			Iterator iter = scalarSelectTokens.iterator();
			int c = 0;
			boolean nolast = false; //real hacky...
			int parenCount = 0; // used to count the nesting of parentheses
			while ( iter.hasNext() ) {
				Object next = iter.next();
				if ( next instanceof String ) {
					String token = ( String ) next;

					if ( "(".equals( token ) ) {
						parenCount++;
					}
					else if ( ")".equals( token ) ) {
						parenCount--;
					}

					String lc = token.toLowerCase();
					if ( lc.equals( ", " ) ) {
						if ( nolast ) {
							nolast = false;
						}
						else {
							if ( !isSubselect && parenCount == 0 ) {
								int x = c++;
								buf.append( " as " )
										.append( NameGenerator.scalarName( x, 0 ) );
							}
						}
					}
					buf.append( token );
					if ( lc.equals( "distinct" ) || lc.equals( "all" ) ) {
						buf.append( ' ' );
					}
				}
				else {
					nolast = true;
					String[] tokens = ( String[] ) next;
					for ( int i = 0; i < tokens.length; i++ ) {
						buf.append( tokens[i] );
						if ( !isSubselect ) {
							buf.append( " as " )
									.append( NameGenerator.scalarName( c, i ) );
						}
						if ( i != tokens.length - 1 ) buf.append( ", " );
					}
					c++;
				}
			}
			if ( !isSubselect && !nolast ) {
				int x = c++;
				buf.append( " as " )
						.append( NameGenerator.scalarName( x, 0 ) );
			}

		}

		return buf.toString();
	}

	private void mergeJoins(JoinFragment ojf) throws MappingException, QueryException {

		Iterator iter = joins.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = ( Map.Entry ) iter.next();
			String name = ( String ) me.getKey();
			JoinSequence join = ( JoinSequence ) me.getValue();
			join.setSelector( new JoinSequence.Selector() {
				public boolean includeSubclasses(String alias) {
					boolean include = returnedTypes.contains( alias ) && !isShallowQuery();
					return include;
				}
			} );

			if ( typeMap.containsKey( name ) ) {
				ojf.addFragment( join.toJoinFragment( enabledFilters, true ) );
			}
			else if ( collections.containsKey( name ) ) {
				ojf.addFragment( join.toJoinFragment( enabledFilters, true ) );
			}
			else {
				//name from a super query (a bit inelegant that it shows up here)
			}

		}

	}

	public final Set<Serializable> getQuerySpaces() {
		return querySpaces;
	}

	/**
	 * Is this query called by scroll() or iterate()?
	 *
	 * @return true if it is, false if it is called by find() or list()
	 */
	boolean isShallowQuery() {
		return shallowQuery;
	}

	void addQuerySpaces(Serializable[] spaces) {
		Collections.addAll( querySpaces, spaces );
		if ( superQuery != null ) superQuery.addQuerySpaces( spaces );
	}

	void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	boolean isSubquery() {
		return superQuery != null;
	}

	/**
	 * Overrides method from Loader
	 */
	@Override
    public CollectionPersister[] getCollectionPersisters() {
		return collectionPersister == null ? null : new CollectionPersister[] { collectionPersister };
	}

	@Override
    protected String[] getCollectionSuffixes() {
		return collectionPersister == null ? null : new String[] { "__" };
	}

	void setCollectionToFetch(String role, String name, String ownerName, String entityName)
			throws QueryException {
		fetchName = name;
		collectionPersister = getCollectionPersister( role );
		collectionOwnerName = ownerName;
		if ( collectionPersister.getElementType().isEntityType() ) {
			addEntityToFetch( entityName );
		}
	}

	@Override
    protected String[] getSuffixes() {
		return suffixes;
	}

	@Override
    protected String[] getAliases() {
		return names;
	}

	/**
	 * Used for collection filters
	 */
	private void addFromAssociation(final String elementName, final String collectionRole)
			throws QueryException {
		//q.addCollection(collectionName, collectionRole);
		QueryableCollection persister = getCollectionPersister( collectionRole );
		Type collectionElementType = persister.getElementType();
		if ( !collectionElementType.isEntityType() ) {
			throw new QueryException( "collection of values in filter: " + elementName );
		}

		String[] keyColumnNames = persister.getKeyColumnNames();
		//if (keyColumnNames.length!=1) throw new QueryException("composite-key collection in filter: " + collectionRole);

		String collectionName;
		JoinSequence join = new JoinSequence( getFactory() );
		collectionName = persister.isOneToMany() ?
				elementName :
				createNameForCollection( collectionRole );
		join.setRoot( persister, collectionName );
		if ( !persister.isOneToMany() ) {
			//many-to-many
			addCollection( collectionName, collectionRole );
			try {
				join.addJoin( ( AssociationType ) persister.getElementType(),
						elementName,
						JoinType.INNER_JOIN,
						persister.getElementColumnNames(collectionName) );
			}
			catch ( MappingException me ) {
				throw new QueryException( me );
			}
		}
		join.addCondition( collectionName, keyColumnNames, " = ?" );
		//if ( persister.hasWhere() ) join.addCondition( persister.getSQLWhereString(collectionName) );
		EntityType elemType = ( EntityType ) collectionElementType;
		addFrom( elementName, elemType.getAssociatedEntityName(), join );

	}

	String getPathAlias(String path) {
		return ( String ) pathAliases.get( path );
	}

	JoinSequence getPathJoin(String path) {
		return ( JoinSequence ) pathJoins.get( path );
	}

	void addPathAliasAndJoin(String path, String alias, JoinSequence joinSequence) {
		pathAliases.put( path, alias );
		pathJoins.put( path, joinSequence );
	}

	@Override
	public List list(SessionImplementor session, QueryParameters queryParameters)
			throws HibernateException {
		return list( session, queryParameters, getQuerySpaces(), actualReturnTypes );
	}

	/**
	 * Return the query results as an iterator
	 */
	@Override
	public Iterator iterate(QueryParameters queryParameters, EventSource session)
			throws HibernateException {

		boolean stats = session.getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) startTime = System.currentTimeMillis();

		try {
			final List<AfterLoadAction> afterLoadActions = new ArrayList<AfterLoadAction>();
			final SqlStatementWrapper wrapper = executeQueryStatement( queryParameters, false, afterLoadActions, session );
			final ResultSet rs = wrapper.getResultSet();
			final PreparedStatement st = (PreparedStatement) wrapper.getStatement();
			HolderInstantiator hi = HolderInstantiator.createClassicHolderInstantiator(holderConstructor, queryParameters.getResultTransformer());
			Iterator result = new IteratorImpl( rs, st, session, queryParameters.isReadOnly( session ), returnTypes, getColumnNames(), hi );

			if ( stats ) {
				session.getFactory().getStatisticsImplementor().queryExecuted(
						"HQL: " + queryString,
						0,
						System.currentTimeMillis() - startTime
					);
			}

			return result;

		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not execute query using iterate",
					getSQLString()
				);
		}

	}

	public int executeUpdate(QueryParameters queryParameters, SessionImplementor session) throws HibernateException {
		throw new UnsupportedOperationException( "Not supported!  Use the AST translator...");
	}

	@Override
    protected boolean[] includeInResultRow() {
		boolean[] isResultReturned = includeInSelect;
		if ( hasScalars ) {
			isResultReturned = new boolean[ returnedTypes.size() ];
			Arrays.fill( isResultReturned, true );
		}
		return isResultReturned;
	}


	@Override
    protected ResultTransformer resolveResultTransformer(ResultTransformer resultTransformer) {
		return HolderInstantiator.resolveClassicResultTransformer(
				holderConstructor,
				resultTransformer
		);
	}

	@Override
    protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {
		Object[] resultRow = getResultRow( row, rs, session );
		return ( holderClass == null && resultRow.length == 1 ?
				resultRow[ 0 ] :
				resultRow
		);
	}

	@Override
    protected Object[] getResultRow(Object[] row, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {
		Object[] resultRow;
		if ( hasScalars ) {
			String[][] scalarColumns = getColumnNames();
			int queryCols = returnTypes.length;
			resultRow = new Object[queryCols];
			for ( int i = 0; i < queryCols; i++ ) {
				resultRow[i] = returnTypes[i].nullSafeGet( rs, scalarColumns[i], session, null );
			}
		}
		else {
			resultRow = toResultRow( row );
		}
		return resultRow;
	}

	@Override
    protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		if ( holderClass != null ) {
			for ( int i = 0; i < results.size(); i++ ) {
				Object[] row = ( Object[] ) results.get( i );
				try {
					results.set( i, holderConstructor.newInstance( row ) );
				}
				catch ( Exception e ) {
					throw new QueryException( "could not instantiate: " + holderClass, e );
				}
			}
		}
		return results;
	}

	private Object[] toResultRow(Object[] row) {
		if ( selectLength == row.length ) {
			return row;
		}
		else {
			Object[] result = new Object[selectLength];
			int j = 0;
			for ( int i = 0; i < row.length; i++ ) {
				if ( includeInSelect[i] ) result[j++] = row[i];
			}
			return result;
		}
	}

	void setHolderClass(Class clazz) {
		holderClass = clazz;
	}

	@Override
    protected LockMode[] getLockModes(LockOptions lockOptions) {

		// unfortunately this stuff can't be cached because
		// it is per-invocation, not constant for the
		// QueryTranslator instance
		HashMap nameLockOptions = new HashMap();
		if ( lockOptions == null) {
			lockOptions = LockOptions.NONE;
		}

		if ( lockOptions.getAliasLockCount() > 0 ) {
			Iterator iter = lockOptions.getAliasLockIterator();
			while ( iter.hasNext() ) {
				Map.Entry me = ( Map.Entry ) iter.next();
				nameLockOptions.put( getAliasName( ( String ) me.getKey() ),
						me.getValue() );
			}
		}
		LockMode[] lockModesArray = new LockMode[names.length];
		for ( int i = 0; i < names.length; i++ ) {
			LockMode lm = ( LockMode ) nameLockOptions.get( names[i] );
			//if ( lm == null ) lm = LockOptions.NONE;
			if ( lm == null ) lm = lockOptions.getLockMode();
			lockModesArray[i] = lm;
		}
		return lockModesArray;
	}

	@Override
    protected String applyLocks(
			String sql,
			QueryParameters parameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) throws QueryException {
		// can't cache this stuff either (per-invocation)
		final LockOptions lockOptions = parameters.getLockOptions();
		final String result;
		if ( lockOptions == null ||
			( lockOptions.getLockMode() == LockMode.NONE && lockOptions.getAliasLockCount() == 0 ) ) {
			return sql;
		}
		else {
			LockOptions locks = new LockOptions();
			locks.setLockMode(lockOptions.getLockMode());
			locks.setTimeOut(lockOptions.getTimeOut());
			locks.setScope(lockOptions.getScope());
			Iterator iter = lockOptions.getAliasLockIterator();
			while ( iter.hasNext() ) {
				Map.Entry me = ( Map.Entry ) iter.next();
				locks.setAliasSpecificLockMode( getAliasName( ( String ) me.getKey() ), (LockMode) me.getValue() );
			}
			Map keyColumnNames = null;
			if ( dialect.forUpdateOfColumns() ) {
				keyColumnNames = new HashMap();
				for ( int i = 0; i < names.length; i++ ) {
					keyColumnNames.put( names[i], persisters[i].getIdentifierColumnNames() );
				}
			}
			result = dialect.applyLocksToSql( sql, locks, keyColumnNames );
		}
		logQuery( queryString, result );
		return result;
	}

	@Override
    protected boolean upgradeLocks() {
		return true;
	}

	@Override
    protected int[] getCollectionOwners() {
		return new int[] { collectionOwnerColumn };
	}

	protected boolean isCompiled() {
		return compiled;
	}

	@Override
    public String toString() {
		return queryString;
	}

	@Override
    protected int[] getOwners() {
		return owners;
	}

	@Override
    protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	public Class getHolderClass() {
		return holderClass;
	}

	public Map getEnabledFilters() {
		return enabledFilters;
	}

	public ScrollableResults scroll(final QueryParameters queryParameters,
									final SessionImplementor session)
			throws HibernateException {
		HolderInstantiator hi = HolderInstantiator.createClassicHolderInstantiator(
				holderConstructor, queryParameters.getResultTransformer()
		);
		return scroll( queryParameters, returnTypes, hi, session );
	}

	@Override
    public String getQueryIdentifier() {
		return queryIdentifier;
	}

	@Override
    protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}

	public void validateScrollability() throws HibernateException {
		// This is the legacy behaviour for HQL queries...
		if ( getCollectionPersisters() != null ) {
			throw new HibernateException( "Cannot scroll queries which initialize collections" );
		}
	}

	public boolean containsCollectionFetches() {
		return false;
	}

	public boolean isManipulationStatement() {
		// classic parser does not support bulk manipulation statements
		return false;
	}

	@Override
	public Class getDynamicInstantiationResultType() {
		return holderClass;
	}

	public ParameterTranslations getParameterTranslations() {
		return new ParameterTranslations() {

			public boolean supportsOrdinalParameterMetadata() {
				// classic translator does not support collection of ordinal
				// param metadata
				return false;
			}

			public int getOrdinalParameterCount() {
				return 0; // not known!
			}

			public int getOrdinalParameterSqlLocation(int ordinalPosition) {
				return 0; // not known!
			}

			public Type getOrdinalParameterExpectedType(int ordinalPosition) {
				return null; // not known!
			}

			public Set getNamedParameterNames() {
				return namedParameters.keySet();
			}

			public int[] getNamedParameterSqlLocations(String name) {
				return getNamedParameterLocs( name );
			}

			public Type getNamedParameterExpectedType(String name) {
				return null; // not known!
			}
		};
	}
}
