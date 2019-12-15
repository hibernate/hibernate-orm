/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.internal.CollectionProperties;
import org.hibernate.hql.internal.CollectionSubqueryFactory;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Delegate that handles the type and join sequence information for a FromElement.
 *
 * @author josh
 */
class FromElementType {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FromElementType.class );

	private FromElement fromElement;
	private EntityType entityType;
	private EntityPersister persister;
	private QueryableCollection queryableCollection;
	private CollectionPropertyMapping collectionPropertyMapping;
	private JoinSequence joinSequence;
	private String collectionSuffix;
	private ParameterSpecification indexCollectionSelectorParamSpec;

	public FromElementType(FromElement fromElement, EntityPersister persister, EntityType entityType) {
		this.fromElement = fromElement;
		this.persister = persister;
		this.entityType = entityType;
		if ( persister != null ) {
			fromElement.setText( ( (Queryable) persister ).getTableName() + " " + getTableAlias() );
		}
	}

	protected FromElementType(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	private String getTableAlias() {
		return fromElement.getTableAlias();
	}

	private String getCollectionTableAlias() {
		return fromElement.getCollectionTableAlias();
	}

	public String getCollectionSuffix() {
		return collectionSuffix;
	}

	public void setCollectionSuffix(String suffix) {
		collectionSuffix = suffix;
	}

	public EntityPersister getEntityPersister() {
		return persister;
	}

	public Type getDataType() {
		if ( persister == null ) {
			if ( queryableCollection == null ) {
				return null;
			}
			return queryableCollection.getType();
		}
		else {
			return entityType;
		}
	}

	public Type getSelectType() {
		if ( entityType == null ) {
			return null;
		}
		boolean shallow = fromElement.getFromClause().getWalker().isShallowQuery();
		return fromElement.getSessionFactoryHelper()
				.getFactory()
				.getTypeResolver()
				.getTypeFactory().manyToOne( entityType.getAssociatedEntityName(), shallow );
	}

	/**
	 * Returns the Hibernate queryable implementation for the HQL class.
	 *
	 * @return the Hibernate queryable implementation for the HQL class.
	 */
	public Queryable getQueryable() {
		return ( persister instanceof Queryable ) ? (Queryable) persister : null;
	}

	/**
	 * Render the identifier select, but in a 'scalar' context (i.e. generate the column alias).
	 *
	 * @param i the sequence of the returned type
	 *
	 * @return the identifier select with the column alias.
	 */
	String renderScalarIdentifierSelect(int i) {
		checkInitialized();

		final String[] idPropertyName = getIdentifierPropertyNames();
		StringBuilder buf = new StringBuilder();
		int counter = 0;
		for ( int j = 0; j < idPropertyName.length; j++ ) {
			String propertyName = idPropertyName[j];
			String[] toColumns = getPropertyMapping( propertyName ).toColumns( getTableAlias(), propertyName );
			for ( int h = 0; h < toColumns.length; h++, counter++ ) {
				String column = toColumns[h];
				if ( j + h > 0 ) {
					buf.append( ", " );
				}
				buf.append( column ).append( " as " ).append( NameGenerator.scalarName( i, counter ) );
			}
		}

		LOG.debug( "Rendered scalar ID select column(s): " + buf );
		return buf.toString();
	}

	/**
	 * Returns the identifier select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k The sequence of the current returned type.
	 *
	 * @return the identifier select SQL fragment.
	 */
	String renderIdentifierSelect(int size, int k) {
		checkInitialized();
		// Render the identifier select fragment using the table alias.
		if ( fromElement.getFromClause().isSubQuery() ) {
			// TODO: Replace this with a more elegant solution.
			String[] idColumnNames = ( persister != null ) ?
					( (Queryable) persister ).getIdentifierColumnNames() : new String[0];
			StringBuilder buf = new StringBuilder();
			for ( int i = 0; i < idColumnNames.length; i++ ) {
				buf.append( fromElement.getTableAlias() ).append( '.' ).append( idColumnNames[i] );
				if ( i != idColumnNames.length - 1 ) {
					buf.append( ", " );
				}
			}
			return buf.toString();
		}
		else {
			if ( persister == null ) {
				throw new QueryException( "not an entity" );
			}
			String fragment = ( (Queryable) persister ).identifierSelectFragment(
					getTableAlias(), getSuffix(
					size,
					k
			)
			);
			return trimLeadingCommaAndSpaces( fragment );
		}
	}

	private String getSuffix(int size, int sequence) {
		return generateSuffix( size, sequence );
	}

	private static String generateSuffix(int size, int k) {
		return size == 1 ? "" : AliasConstantsHelper.get( k );
	}

	private void checkInitialized() {
		fromElement.checkInitialized();
	}

	/**
	 * Returns the property select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k The sequence of the current returned type.
	 *
	 * @return the property select SQL fragment.
	 */
	String renderPropertySelect(int size, int k, boolean allProperties) {
		checkInitialized();
		if ( persister == null ) {
			return "";
		}
		else {
			String fragment = ( (Queryable) persister ).propertySelectFragment(
					getTableAlias(),
					getSuffix( size, k ),
					allProperties
			);
			return trimLeadingCommaAndSpaces( fragment );
		}
	}

	public String renderMapKeyPropertySelectFragment(int size, int k) {
		if ( persister == null ) {
			throw new IllegalStateException( "Unexpected state in call to renderMapKeyPropertySelectFragment" );
		}

		final String fragment = ( (Queryable) persister ).propertySelectFragment(
				getTableAlias(),
				getSuffix( size, k ),
				false
		);
		return trimLeadingCommaAndSpaces( fragment );

//		if ( queryableCollection == null
//				|| !Map.class.isAssignableFrom( queryableCollection.getCollectionType().getReturnedClass() ) ) {
//			throw new IllegalStateException( "Illegal call to renderMapKeyPropertySelectFragment() when FromElement is not a Map" );
//		}
//
//		if ( !queryableCollection.getIndexType().isEntityType() ) {
//			return null;
//		}
//
//		final HqlSqlWalker walker = fromElement.getWalker();
//		final SessionFactoryHelper sfh = walker.getSessionFactoryHelper();
//		final SessionFactoryImplementor sf = sfh.getFactory();
//
//		final EntityType indexEntityType = (EntityType) queryableCollection.getIndexType();
//		final EntityPersister indexEntityPersister = (EntityPersister) indexEntityType.getAssociatedJoinable( sf );
//
//		final String fragment = ( (Queryable) indexEntityPersister ).propertySelectFragment(
//				getTableAlias(),
//				getSuffix( size, k ),
//				false
//		);
//		return trimLeadingCommaAndSpaces( fragment );
	}

	public String renderMapEntryPropertySelectFragment(int size, int k) {
		return null;
	}

	String renderCollectionSelectFragment(int size, int k) {
		if ( queryableCollection == null ) {
			return "";
		}
		else {
			if ( collectionSuffix == null ) {
				collectionSuffix = generateSuffix( size, k );
			}
			String fragment = queryableCollection.selectFragment( getCollectionTableAlias(), collectionSuffix );
			return trimLeadingCommaAndSpaces( fragment );
		}
	}

	public String renderValueCollectionSelectFragment(int size, int k) {
		if ( queryableCollection == null ) {
			return "";
		}
		else {
			if ( collectionSuffix == null ) {
				collectionSuffix = generateSuffix( size, k );
			}
			String fragment = queryableCollection.selectFragment( getTableAlias(), collectionSuffix );
			return trimLeadingCommaAndSpaces( fragment );
		}
	}

	/**
	 * This accounts for a quirk in Queryable, where it sometimes generates ',  ' in front of the
	 * SQL fragment.  :-P
	 *
	 * @param fragment An SQL fragment.
	 *
	 * @return The fragment, without the leading comma and spaces.
	 */
	private static String trimLeadingCommaAndSpaces(String fragment) {
		if ( fragment.length() > 0 && fragment.charAt( 0 ) == ',' ) {
			fragment = fragment.substring( 1 );
		}
		fragment = fragment.trim();
		return fragment.trim();
	}


	public void setJoinSequence(JoinSequence joinSequence) {
		this.joinSequence = joinSequence;
		joinSequence.applyTreatAsDeclarations( treatAsDeclarations );
	}

	public JoinSequence getJoinSequence() {
		if ( joinSequence != null ) {
			return joinSequence;
		}

		// Class names in the FROM clause result in a JoinSequence (the old FromParser does this).
		if ( persister instanceof Joinable ) {
			Joinable joinable = (Joinable) persister;
			final JoinSequence joinSequence = fromElement.getSessionFactoryHelper().createJoinSequence().setRoot(
					joinable,
					getTableAlias()
			);
			joinSequence.applyTreatAsDeclarations( treatAsDeclarations );
			return joinSequence;
		}
		else {
			// TODO: Should this really return null?  If not, figure out something better to do here.
			return null;
		}
	}

	private Set<String> treatAsDeclarations;

	public void applyTreatAsDeclarations(Set<String> treatAsDeclarations) {
		if ( treatAsDeclarations != null && !treatAsDeclarations.isEmpty() ) {
			if ( this.treatAsDeclarations == null ) {
				this.treatAsDeclarations = new HashSet<String>();
			}

			for ( String treatAsSubclassName : treatAsDeclarations ) {
				try {
					EntityPersister subclassPersister = fromElement.getSessionFactoryHelper().requireClassPersister(
							treatAsSubclassName
					);
					this.treatAsDeclarations.add( subclassPersister.getEntityName() );
				}
				catch (SemanticException e) {
					throw new QueryException( "Unable to locate persister for subclass named in TREAT-AS : " + treatAsSubclassName );
				}
			}

			if ( joinSequence != null ) {
				joinSequence.applyTreatAsDeclarations( this.treatAsDeclarations );
			}
		}
	}

	public void setQueryableCollection(QueryableCollection queryableCollection) {
		if ( this.queryableCollection != null ) {
			throw new IllegalStateException( "QueryableCollection is already defined for " + this + "!" );
		}
		this.queryableCollection = queryableCollection;
		if ( !queryableCollection.isOneToMany() ) {
			// For many-to-many joins, use the tablename from the queryable collection for the default text.
			fromElement.setText( queryableCollection.getTableName() + " " + getTableAlias() );
		}
	}

	public QueryableCollection getQueryableCollection() {
		return queryableCollection;
	}

	public String getPropertyTableName(String propertyName) {
		checkInitialized();
		if ( this.persister != null ) {
			AbstractEntityPersister aep = (AbstractEntityPersister) this.persister;
			try {
				return aep.getSubclassTableName( aep.getSubclassPropertyTableNumber( propertyName ) );
			}
			catch (QueryException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Returns the type of a property, given it's name (the last part) and the full path.
	 *
	 * @param propertyName The last part of the full path to the property.
	 *
	 * @return The type.
	 *
	 * @0param getPropertyPath The full property path.
	 */
	public Type getPropertyType(String propertyName, String propertyPath) {
		checkInitialized();
		Type type = null;
		// If this is an entity and the property is the identifier property, then use getIdentifierType().
		//      Note that the propertyName.equals( getPropertyPath ) checks whether we have a component
		//      key reference, where the component class property name is the same as the
		//      entity id property name; if the two are not equal, this is the case and
		//      we'd need to "fall through" to using the property mapping.
		if ( persister != null && propertyName.equals( propertyPath ) && propertyName.equals( persister.getIdentifierPropertyName() ) ) {
			type = persister.getIdentifierType();
		}
		else {    // Otherwise, use the property mapping.
			PropertyMapping mapping = getPropertyMapping( propertyName );
			type = mapping.toType( propertyPath );
		}
		if ( type == null ) {
			throw new MappingException(
					"Property " + propertyName + " does not exist in " +
							( ( queryableCollection == null ) ? "class" : "collection" ) + " "
							+ ( ( queryableCollection == null ) ?
							fromElement.getClassName() :
							queryableCollection.getRole() )
			);
		}
		return type;
	}

	String[] toColumns(String tableAlias, String path, boolean inSelect) {
		return toColumns( tableAlias, path, inSelect, false );
	}

	String[] toColumns(String tableAlias, String path, boolean inSelect, boolean forceAlias) {
		checkInitialized();
		PropertyMapping propertyMapping = getPropertyMapping( path );

		if ( !inSelect && queryableCollection != null && CollectionProperties.isCollectionProperty( path ) ) {
			// If this from element is a collection and the path is a collection property (maxIndex, etc.)
			// requiring a sub-query then generate a sub-query.
			//h
			// Unless we are in the select clause, because some dialects do not support
			// Note however, that some dialects do not  However, in the case of this being a collection property reference being in the select, not generating the subquery
			// will not generally work.  The specific cases I am thinking about are the minIndex, maxIndex
			// (most likely minElement, maxElement as well) cases.
			//	todo : if ^^ is the case we should thrown an exception here rather than waiting for the sql error
			//		if the dialect supports select-clause subqueries we could go ahead and generate the subquery also

			// we also need to account for cases where the property name is a CollectionProperty, but
			// is also a property on the element-entity.  This is handled inside `#getPropertyMapping`
			// already; here just check for propertyMapping being the same as the entity persister.  Yes
			// this is hacky, but really this is difficult to handle given the current codebase.
			if ( persister != propertyMapping ) {
				// we want the subquery...
//				DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfCollectionPropertiesInHql( path, fromElement.getClassAlias() );
				return getCollectionPropertyReference( path ).toColumns( tableAlias );
			}
		}

		if ( forceAlias ) {
			return propertyMapping.toColumns( tableAlias, path );
		}

		if ( fromElement.getWalker().getStatementType() == HqlSqlTokenTypes.SELECT ) {
			return propertyMapping.toColumns( tableAlias, path );
		}

		if ( fromElement.getWalker().isSubQuery() ) {
			// for a subquery, the alias to use depends on a few things (we
			// already know this is not an overall SELECT):
			// 1) if this FROM_ELEMENT represents a correlation to the
			// outer-most query
			// A) if the outer query represents a multi-table
			// persister, we need to use the given alias
			// in anticipation of one of the multi-table
			// executors being used (as this subquery will
			// actually be used in the "id select" phase
			// of that multi-table executor)
			// for update queries, the real table name of the updated table must be used if not in the top level where
			// clause, typically in a SET clause
			// B) otherwise, we need to use the persister's
			// table name as the column qualification
			// 2) otherwise (not correlated), use the given alias
			if ( isCorrelation() ) {
				if ( isMultiTable() && ( !isUpdateQuery() || inWhereClause() ) ) {
					return propertyMapping.toColumns( tableAlias, path );
				}
				else if ( isInsertQuery() ) {
					return propertyMapping.toColumns( tableAlias, path );
				}
				return propertyMapping.toColumns( extractTableName(), path );
			}
			return propertyMapping.toColumns( tableAlias, path );
		}

		if ( fromElement.getWalker().getCurrentTopLevelClauseType() == HqlSqlTokenTypes.SELECT ) {
			return propertyMapping.toColumns( tableAlias, path );
		}

		if ( isManipulationQuery() && isMultiTable() && inWhereClause() ) {
			// the actual where-clause will end up being ripped out the update/delete and used in
			// a select to populate the temp table, so its ok to use the table alias to qualify the table refs
			// and safer to do so to protect from same-named columns
			return propertyMapping.toColumns( tableAlias, path );
		}

		String[] columns = propertyMapping.toColumns( path );
		LOG.tracev( "Using non-qualified column reference [{0} -> ({1})]", path, ArrayHelper.toString( columns ) );
		return columns;
	}

	private boolean isCorrelation() {
		FromClause top = fromElement.getWalker().getFinalFromClause();
		return fromElement.getFromClause() != fromElement.getWalker().getCurrentFromClause() &&
				fromElement.getFromClause() == top;
	}

	private boolean isMultiTable() {
		// should be safe to only ever expect EntityPersister references here
		return fromElement.getQueryable() != null &&
				fromElement.getQueryable().isMultiTable();
	}

	private String extractTableName() {
		// should be safe to only ever expect EntityPersister references here
		return fromElement.getQueryable().getTableName();
	}

	private boolean isInsertQuery() {
		return fromElement.getWalker().getStatementType() == HqlSqlTokenTypes.INSERT;
	}

	private boolean isUpdateQuery() {
		return fromElement.getWalker().getStatementType() == HqlSqlTokenTypes.UPDATE;
	}

	private boolean isManipulationQuery() {
		return fromElement.getWalker().getStatementType() == HqlSqlTokenTypes.UPDATE
				|| fromElement.getWalker().getStatementType() == HqlSqlTokenTypes.DELETE;
	}

	private boolean inWhereClause() {
		return fromElement.getWalker().getCurrentTopLevelClauseType() == HqlSqlTokenTypes.WHERE;
	}

	private static final List SPECIAL_MANY2MANY_TREATMENT_FUNCTION_NAMES = java.util.Arrays.asList(
			CollectionPropertyNames.COLLECTION_INDEX,
			CollectionPropertyNames.COLLECTION_MIN_INDEX,
			CollectionPropertyNames.COLLECTION_MAX_INDEX
	);

	PropertyMapping getPropertyMapping(String propertyName) {
		checkInitialized();
		if ( queryableCollection == null ) {        // Not a collection?
			return (PropertyMapping) persister;    // Return the entity property mapping.
		}

		// indexed, many-to-many collections must be treated specially here if the property to
		// be mapped touches on the index as we must adjust the alias to use the alias from
		// the association table (which i different than the one passed in)
		if ( queryableCollection.isManyToMany()
				&& queryableCollection.hasIndex()
				&& SPECIAL_MANY2MANY_TREATMENT_FUNCTION_NAMES.contains( propertyName ) ) {
			return new SpecialManyToManyCollectionPropertyMapping();
		}

		// If the property is a special collection property name, return a CollectionPropertyMapping.
		if ( CollectionProperties.isCollectionProperty( propertyName ) ) {
			if ( collectionPropertyMapping == null ) {
				// lets additionally make sure that the property name is not also the name
				// of a property on the element, assuming that the element is an entity.
				// todo : also consider composites?
				if ( persister != null ) {
					try {
						if ( persister.getPropertyType( propertyName ) != null ) {
							return (PropertyMapping) persister;
						}
					}
					catch (QueryException ignore) {
					}
				}
				collectionPropertyMapping = new CollectionPropertyMapping( queryableCollection );
			}
			return collectionPropertyMapping;
		}

		if ( queryableCollection.getElementType().isAnyType() ) {
			// collection of <many-to-any/> mappings...
			// used to circumvent the component-collection check below...
			return queryableCollection;

		}

		if ( queryableCollection.getElementType().isComponentType() ) {
			// Collection of components.
			if ( propertyName.equals( EntityPersister.ENTITY_ID ) ) {
				return (PropertyMapping) queryableCollection.getOwnerEntityPersister();
			}
		}
		return queryableCollection;
	}

	public boolean isCollectionOfValuesOrComponents() {
		return persister == null
				&& queryableCollection != null
				&& !queryableCollection.getElementType().isEntityType();
	}

	public boolean isEntity() {
		return persister != null;
	}

	public ParameterSpecification getIndexCollectionSelectorParamSpec() {
		return indexCollectionSelectorParamSpec;
	}

	public void setIndexCollectionSelectorParamSpec(ParameterSpecification indexCollectionSelectorParamSpec) {
		this.indexCollectionSelectorParamSpec = indexCollectionSelectorParamSpec;
	}

	public CollectionPropertyReference getCollectionPropertyReference(final String propertyName) {
		if ( queryableCollection == null ) {
			throw new QueryException( "Not a collection reference" );
		}

		final PropertyMapping collectionPropertyMapping;

		if ( queryableCollection.isManyToMany()
				&& queryableCollection.hasIndex()
				&& SPECIAL_MANY2MANY_TREATMENT_FUNCTION_NAMES.contains( propertyName ) ) {
			collectionPropertyMapping = new SpecialManyToManyCollectionPropertyMapping();
		}
		else if ( CollectionProperties.isCollectionProperty( propertyName ) ) {
			if ( this.collectionPropertyMapping == null ) {
				this.collectionPropertyMapping = new CollectionPropertyMapping( queryableCollection );
			}
			collectionPropertyMapping = this.collectionPropertyMapping;
		}
		else {
			collectionPropertyMapping = queryableCollection;
		}

		return new CollectionPropertyReference() {
			@Override
			public Type getType() {
				return collectionPropertyMapping.toType( propertyName );
			}

			@Override
			public String[] toColumns(final String tableAlias) {
				if ( propertyName.equalsIgnoreCase( "index" ) ) {
					return collectionPropertyMapping.toColumns( tableAlias, propertyName );
				}

				Map enabledFilters = fromElement.getWalker().getEnabledFilters();
				String subquery = CollectionSubqueryFactory.createCollectionSubquery(
						joinSequence.copyForCollectionProperty().setUseThetaStyle( true ),
						enabledFilters,
						collectionPropertyMapping.toColumns( tableAlias, propertyName )
				);
				LOG.debugf( "toColumns(%s,%s) : subquery = %s", tableAlias, propertyName, subquery );
				return new String[] {"(" + subquery + ")"};
			}
		};
	}

	private class SpecialManyToManyCollectionPropertyMapping implements PropertyMapping {
		@Override
		public Type getType() {
			return queryableCollection.getCollectionType();
		}

		private void validate(String propertyName) {
			if ( !( CollectionPropertyNames.COLLECTION_INDEX.equals( propertyName )
					|| CollectionPropertyNames.COLLECTION_MAX_INDEX.equals( propertyName )
					|| CollectionPropertyNames.COLLECTION_MIN_INDEX.equals( propertyName ) ) ) {
				throw new IllegalArgumentException( "Expecting index-related function call" );
			}
		}

		@Override
		public Type toType(String propertyName) throws QueryException {
			validate( propertyName );
			return queryableCollection.getIndexType();
		}

		@Override
		public String[] toColumns(String alias, String propertyName) throws QueryException {
			validate( propertyName );
			final String joinTableAlias = joinSequence.getFirstJoin().getAlias();
			if ( CollectionPropertyNames.COLLECTION_INDEX.equals( propertyName ) ) {
				return queryableCollection.toColumns( joinTableAlias, propertyName );
			}

			final String[] cols = queryableCollection.getIndexColumnNames( joinTableAlias );
			if ( CollectionPropertyNames.COLLECTION_MIN_INDEX.equals( propertyName ) ) {
				if ( cols.length != 1 ) {
					throw new QueryException( "composite collection index in minIndex()" );
				}
				return new String[] {"min(" + cols[0] + ')'};
			}
			else {
				if ( cols.length != 1 ) {
					throw new QueryException( "composite collection index in maxIndex()" );
				}
				return new String[] {"max(" + cols[0] + ')'};
			}
		}

		@Override
		public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
			validate( propertyName );
			return queryableCollection.toColumns( propertyName );
		}
	}

	public String[] getIdentifierPropertyNames() {
		if ( getEntityPersister() != null ) {
			String identifierPropertyName = getEntityPersister().getIdentifierPropertyName();
			if ( identifierPropertyName != null ) {
				return new String[] { identifierPropertyName };
			}
			else {
				final IdentifierProperty identifierProperty = getEntityPersister().getEntityMetamodel()
						.getIdentifierProperty();
				if ( identifierProperty.hasIdentifierMapper() && !identifierProperty.isEmbedded() ) {
					return new String[] { PropertyPath.IDENTIFIER_MAPPER_PROPERTY };
				}
				else {
					if ( EmbeddedComponentType.class.isInstance( identifierProperty.getType() ) ) {
						return ( (EmbeddedComponentType) identifierProperty.getType() ).getPropertyNames();
					}
				}
			}
		}
		return new String[] { EntityPersister.ENTITY_ID };
	}
}
