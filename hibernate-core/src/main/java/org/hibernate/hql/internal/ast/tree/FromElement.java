/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.internal.CollectionProperties;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.TypeDiscriminatorMetadata;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.DynamicFilterParameterSpecification;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.DiscriminatorMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Represents a single mapped class mentioned in an HQL FROM clause.  Each
 * class reference will have the following symbols:
 * <ul>
 * <li>A class name - This is the name of the Java class that is mapped by Hibernate.</li>
 * <li>[optional] an HQL alias for the mapped class.</li>
 * <li>A table name - The name of the table that is mapped to the Java class.</li>
 * <li>A table alias - The alias for the table that will be used in the resulting SQL.</li>
 * </ul>
 *
 * @author josh
 */
public class FromElement extends HqlSqlWalkerNode implements DisplayableNode, ParameterContainer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( FromElement.class );

	private String className;
	private String classAlias;
	private String tableAlias;
	private String collectionTableAlias;
	private FromClause fromClause;
	private boolean includeSubclasses = true;
	private boolean collectionJoin;
	private FromElement origin;
	private String[] columns;
	private String role;
	private boolean fetch;
	private boolean isAllPropertyFetch;
	private boolean filter;
	private int sequence = -1;
	private boolean useFromFragment;
	private boolean initialized;
	private FromElementType elementType;
	private boolean useWhereFragment = true;
	private List<FromElement> destinations;
	private boolean manyToMany;
	private String withClauseFragment;
	private String withClauseJoinAlias;
	private String withClauseCollectionJoinAlias;
	private boolean dereferencedBySuperclassProperty;
	private boolean dereferencedBySubclassProperty;

	public FromElement() {
	}

	/**
	 * Constructor form used to initialize {@link ComponentJoin}
	 *
	 * @param fromClause The FROM clause to which this element belongs
	 * @param origin The origin (LHS) of this element
	 * @param alias The alias applied to this element
	 */
	protected FromElement(
			FromClause fromClause,
			FromElement origin,
			String alias) {
		this.fromClause = fromClause;
		this.origin = origin;
		this.classAlias = alias;
		this.tableAlias = origin.getTableAlias();
		super.initialize( fromClause.getWalker() );

	}

	protected void initializeComponentJoin(FromElementType elementType) {
		fromClause.registerFromElement( this );
		elementType.applyTreatAsDeclarations( getWalker().getTreatAsDeclarationsByPath( classAlias ) );
		this.elementType = elementType;
		initialized = true;
	}

	public String getCollectionSuffix() {
		return elementType.getCollectionSuffix();
	}

	public void setCollectionSuffix(String suffix) {
		elementType.setCollectionSuffix(suffix);
	}

	public void initializeCollection(FromClause fromClause, String classAlias, String tableAlias) {
		doInitialize( fromClause, tableAlias, null, classAlias, null, null );
		initialized = true;
	}

	public void initializeEntity(
			FromClause fromClause,
			String className,
			EntityPersister persister,
			EntityType type,
			String classAlias,
			String tableAlias) {
		doInitialize( fromClause, tableAlias, className, classAlias, persister, type );
		this.sequence = fromClause.nextFromElementCounter();
		initialized = true;
	}

	protected void doInitialize(
			FromClause fromClause,
			String tableAlias,
			String className,
			String classAlias,
			EntityPersister persister,
			EntityType type) {
		if ( initialized ) {
			throw new IllegalStateException( "Already initialized!!" );
		}
		this.fromClause = fromClause;
		this.tableAlias = tableAlias;
		this.className = className;
		this.classAlias = classAlias;
		this.elementType = new FromElementType( this, persister, type );
		// Register the FromElement with the FROM clause, now that we have the names and aliases.
		fromClause.registerFromElement( this );
		LOG.debugf( "%s : %s (%s) -> %s", fromClause, className, classAlias == null ? "<no alias>" : classAlias, tableAlias );
	}

	public EntityPersister getEntityPersister() {
		return elementType.getEntityPersister();
	}

	@Override
	public Type getDataType() {
		return elementType.getDataType();
	}

	public Type getSelectType() {
		return elementType.getSelectType();
	}

	public Queryable getQueryable() {
		return elementType.getQueryable();
	}

	public String getClassName() {
		return className;
	}

	public String getClassAlias() {
		return classAlias;
		//return classAlias == null ? className : classAlias;
	}

	private String getTableName() {
		Queryable queryable = getQueryable();
		return ( queryable != null ) ? queryable.getTableName() : "{none}";
	}

	public String getTableAlias() {
		return tableAlias;
	}

	/**
	 * Render the identifier select, but in a 'scalar' context (i.e. generate the column alias).
	 *
	 * @param i the sequence of the returned type
	 * @return the identifier select with the column alias.
	 */
	String renderScalarIdentifierSelect(int i) {
		return elementType.renderScalarIdentifierSelect( i );
	}

	void checkInitialized() {
		if ( !initialized ) {
			throw new IllegalStateException( "FromElement has not been initialized!" );
		}
	}

	/**
	 * Returns the identifier select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k    The sequence of the current returned type.
	 * @return the identifier select SQL fragment.
	 */
	String renderIdentifierSelect(int size, int k) {
		return elementType.renderIdentifierSelect( size, k );
	}

	/**
	 * Returns the property select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k    The sequence of the current returned type.
	 * @return the property select SQL fragment.
	 */
	String renderPropertySelect(int size, int k) {
		return elementType.renderPropertySelect( size, k, isAllPropertyFetch );
	}

	public String renderMapKeyPropertySelectFragment(int size, int k) {
		return elementType.renderMapKeyPropertySelectFragment( size, k );
	}

	public String renderMapEntryPropertySelectFragment(int size, int k) {
		return elementType.renderMapEntryPropertySelectFragment( size, k );
	}

	String renderCollectionSelectFragment(int size, int k) {
		return elementType.renderCollectionSelectFragment( size, k );
	}

	String renderValueCollectionSelectFragment(int size, int k) {
		return elementType.renderValueCollectionSelectFragment( size, k );
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	/**
	 * Returns true if this FromElement was implied by a path, or false if this FROM element is explicitly declared in
	 * the FROM clause.
	 *
	 * @return true if this FromElement was implied by a path, or false if this FROM element is explicitly declared
	 */
	public boolean isImplied() {
		return false;	// This is an explicit FROM element.
	}

	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	public String getDisplayText() {
		StringBuilder buf = new StringBuilder();
		buf.append( "FromElement{" );
		appendDisplayText( buf );
		buf.append( "}" );
		return buf.toString();
	}

	protected void appendDisplayText(StringBuilder buf) {
		buf.append( isImplied() ? (
				isImpliedInFromClause() ? "implied in FROM clause" : "implied" )
				: "explicit" );
		buf.append( "," ).append( isCollectionJoin() ? "collection join" : "not a collection join" );
		buf.append( "," ).append( fetch ? "fetch join" : "not a fetch join" );
		buf.append( "," ).append( isAllPropertyFetch ? "fetch all properties" : "fetch non-lazy properties" );
		buf.append( ",classAlias=" ).append( getClassAlias() );
		buf.append( ",role=" ).append( role );
		buf.append( ",tableName=" ).append( getTableName() );
		buf.append( ",tableAlias=" ).append( getTableAlias() );
		FromElement origin = getRealOrigin();
		buf.append( ",origin=" ).append( origin == null ? "null" : origin.getText() );
		buf.append( ",columns={" );
		if ( columns != null ) {
			for ( int i = 0; i < columns.length; i++ ) {
				buf.append( columns[i] );
				if ( i < columns.length ) {
					buf.append( " " );
				}
			}
		}
		buf.append( ",className=" ).append( className );
		buf.append( "}" );
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals( obj );
	}


	public void setJoinSequence(JoinSequence joinSequence) {
		elementType.setJoinSequence( joinSequence );
	}

	public JoinSequence getJoinSequence() {
		return elementType.getJoinSequence();
	}

	public void setIncludeSubclasses(boolean includeSubclasses) {
		if ( !includeSubclasses && isDereferencedBySuperclassOrSubclassProperty() && LOG.isTraceEnabled() ) {
			LOG.trace( "Attempt to disable subclass-inclusions : ", new Exception( "Stack-trace source" ) );
		}
		this.includeSubclasses = includeSubclasses;
	}

	public boolean isIncludeSubclasses() {
		return includeSubclasses;
	}

	public boolean isDereferencedBySuperclassOrSubclassProperty() {
		return dereferencedBySubclassProperty || dereferencedBySuperclassProperty;
	}

	public String getIdentityColumn() {
		final String[] cols = getIdentityColumns();
		if ( cols.length == 1 ) {
			return cols[0];
		}
		else {
			return "(" + StringHelper.join( ", ", cols ) + ")";
		}
	}

	public String[] getIdentityColumns() {
		checkInitialized();
		final String table = getTableAlias();
		if ( table == null ) {
			throw new IllegalStateException( "No table alias for node " + this );
		}

		final String propertyName = getIdentifierPropertyName();

		if ( getWalker().getStatementType() == HqlSqlTokenTypes.SELECT ) {
			return getPropertyMapping( propertyName ).toColumns( table, propertyName );
		}
		else {
			return getPropertyMapping( propertyName ).toColumns( propertyName );
		}
	}

	public void setCollectionJoin(boolean collectionJoin) {
		this.collectionJoin = collectionJoin;
	}

	public boolean isCollectionJoin() {
		return collectionJoin;
	}

	public void setRole(String role) {
		this.role = role;
		applyTreatAsDeclarations( getWalker().getTreatAsDeclarationsByPath( role ) );
	}

	public void applyTreatAsDeclarations(Set<String> treatAsDeclarationsByPath) {
		elementType.applyTreatAsDeclarations( treatAsDeclarationsByPath );
	}

	public String getRole() {
		return role;
	}

	public void setQueryableCollection(QueryableCollection queryableCollection) {
		elementType.setQueryableCollection( queryableCollection );
	}

	public QueryableCollection getQueryableCollection() {
		return elementType.getQueryableCollection();
	}

	public void setColumns(String[] columns) {
		this.columns = columns;
	}

	public void setOrigin(FromElement origin, boolean manyToMany) {
		this.origin = origin;
		this.manyToMany = manyToMany;
		origin.addDestination( this );
		if ( origin.getFromClause() == this.getFromClause() ) {
			// TODO: Figure out a better way to get the FROM elements in a proper tree structure.
			// If this is not the destination of a many-to-many, add it as a child of the origin.
			if ( manyToMany ) {
				ASTUtil.appendSibling( origin, this );
			}
			else {
				if ( !getWalker().isInFrom() && !getWalker().isInSelect() && !getWalker().isInEntityGraph() ) {
					getFromClause().addChild( this );
				}
				else {
					origin.addChild( this );
				}
			}
		}
		else if ( !getWalker().isInFrom() ) {
			// HHH-276 : implied joins in a subselect where clause - The destination needs to be added
			// to the destination's from clause.
			getFromClause().addChild( this );	// Not sure if this is will fix everything, but it works.
		}
		else {
			// Otherwise, the destination node was implied by the FROM clause and the FROM clause processor
			// will automatically add it in the right place.
		}
	}

	public boolean isManyToMany() {
		return manyToMany;
	}

	private void addDestination(FromElement fromElement) {
		if ( destinations == null ) {
			destinations = new LinkedList<FromElement>();
		}
		destinations.add( fromElement );
	}

	public List<FromElement> getDestinations() {
		if ( destinations == null ) {
			return Collections.emptyList();
		}
		else {
			return destinations;
		}
	}

	public FromElement getOrigin() {
		return origin;
	}

	public FromElement getRealOrigin() {
		if ( origin == null ) {
			return null;
		}
		if ( origin.getText() == null || "".equals( origin.getText() ) ) {
			return origin.getRealOrigin();
		}
		return origin;
	}

	public FromElement getFetchOrigin() {
		if ( origin == null ) {
			return null;
		}
		if ( !origin.isFetch() ) {
			return origin;
		}
		if ( origin.getText() == null || "".equals( origin.getText() ) ) {
			return origin.getFetchOrigin();
		}
		return origin;
	}

	public static final String DISCRIMINATOR_PROPERTY_NAME = "class";
	private TypeDiscriminatorMetadata typeDiscriminatorMetadata;

	private static class TypeDiscriminatorMetadataImpl implements TypeDiscriminatorMetadata {
		private final DiscriminatorMetadata persisterDiscriminatorMetadata;
		private final String alias;

		private TypeDiscriminatorMetadataImpl(
				DiscriminatorMetadata persisterDiscriminatorMetadata,
				String alias) {
			this.persisterDiscriminatorMetadata = persisterDiscriminatorMetadata;
			this.alias = alias;
		}

		@Override
		public String getSqlFragment() {
			return persisterDiscriminatorMetadata.getSqlFragment( alias );
		}

		@Override
		public Type getResolutionType() {
			return persisterDiscriminatorMetadata.getResolutionType();
		}
	}

	public TypeDiscriminatorMetadata getTypeDiscriminatorMetadata() {
		if ( typeDiscriminatorMetadata == null ) {
			typeDiscriminatorMetadata = buildTypeDiscriminatorMetadata();
		}
		return typeDiscriminatorMetadata;
	}

	private TypeDiscriminatorMetadata buildTypeDiscriminatorMetadata() {
		final String aliasToUse = getTableAlias();
		Queryable queryable = getQueryable();
		if ( queryable == null ) {
			QueryableCollection collection = getQueryableCollection();
			if ( ! collection.getElementType().isEntityType() ) {
				throw new QueryException( "type discrimination cannot be applied to value collection [" + collection.getRole() + "]" );
			}
			queryable = (Queryable) collection.getElementPersister();
		}

		handlePropertyBeingDereferenced( getDataType(), DISCRIMINATOR_PROPERTY_NAME );

		return new TypeDiscriminatorMetadataImpl( queryable.getTypeDiscriminatorMetadata(), aliasToUse );
	}

	public Type getPropertyType(String propertyName, String propertyPath) {
		return elementType.getPropertyType( propertyName, propertyPath );
	}

	public String[] toColumns(String tableAlias, String path, boolean inSelect) {
		return elementType.toColumns( tableAlias, path, inSelect );
	}

	public String[] toColumns(String tableAlias, String path, boolean inSelect, boolean forceAlias) {
		return elementType.toColumns( tableAlias, path, inSelect, forceAlias );
	}

	public PropertyMapping getPropertyMapping(String propertyName) {
		return elementType.getPropertyMapping( propertyName );
	}

	public CollectionPropertyReference getCollectionPropertyReference(String propertyName) {
		return elementType.getCollectionPropertyReference( propertyName );
	}

	public String getIdentifierPropertyName() {
		return elementType.getIdentifierPropertyName();
	}

	public void setFetch(boolean fetch) {
		this.fetch = fetch;
		// Fetch can't be used with scroll() or iterate().
		if ( fetch && getWalker().isShallowQuery() ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FETCH_WITH_ITERATE );
		}
	}

	public boolean isFetch() {
		return fetch;
	}

	public int getSequence() {
		return sequence;
	}

	public void setFilter(boolean b) {
		filter = b;
	}

	public boolean isFilter() {
		return filter;
	}

	public boolean useFromFragment() {
		checkInitialized();
		// If it's not implied or it is implied and it's a many to many join where the target wasn't found.
		return !isImplied() || this.useFromFragment;
	}

	public void setUseFromFragment(boolean useFromFragment) {
		this.useFromFragment = useFromFragment;
	}

	public boolean useWhereFragment() {
		return useWhereFragment;
	}

	public void setUseWhereFragment(boolean b) {
		useWhereFragment = b;
	}


	public void setCollectionTableAlias(String collectionTableAlias) {
		this.collectionTableAlias = collectionTableAlias;
	}

	public String getCollectionTableAlias() {
		return collectionTableAlias;
	}

	public boolean isCollectionOfValuesOrComponents() {
		return elementType.isCollectionOfValuesOrComponents();
	}

	public boolean isEntity() {
		return elementType.isEntity();
	}

	public void setImpliedInFromClause(boolean flag) {
		throw new UnsupportedOperationException( "Explicit FROM elements can't be implied in the FROM clause!" );
	}

	public boolean isImpliedInFromClause() {
		return false;	// Since this is an explicit FROM element, it can't be implied in the FROM clause.
	}

	public void setInProjectionList(boolean inProjectionList) {
		// Do nothing, eplicit from elements are *always* in the projection list.
	}

	public boolean inProjectionList() {
		return !isImplied() && isFromOrJoinFragment();
	}

	public boolean isFromOrJoinFragment() {
		return getType() == SqlTokenTypes.FROM_FRAGMENT
				|| getType() == SqlTokenTypes.JOIN_FRAGMENT
				|| getType() == SqlTokenTypes.ENTITY_JOIN;
	}

	public boolean isAllPropertyFetch() {
		return isAllPropertyFetch;
	}

	public void setAllPropertyFetch(boolean fetch) {
		isAllPropertyFetch = fetch;
	}

	public String getWithClauseFragment() {
		return withClauseFragment;
	}

	public String getWithClauseJoinAlias() {
		return withClauseJoinAlias;
	}

	public String getWithClauseCollectionJoinAlias() {
		return withClauseCollectionJoinAlias;
	}

	public void setWithClauseFragment(String withClauseJoinAlias, String withClauseCollectionJoinAlias, String withClauseFragment) {
		this.withClauseJoinAlias = withClauseJoinAlias;
		this.withClauseCollectionJoinAlias = withClauseCollectionJoinAlias;
		this.withClauseFragment = withClauseFragment;
	}

	public boolean hasCacheablePersister() {
		if ( getQueryableCollection() != null ) {
			return getQueryableCollection().hasCache();
		}
		else {
			return getQueryable().hasCache();
		}
	}

	public void handlePropertyBeingDereferenced(Type propertySource, String propertyName) {
		if ( getQueryableCollection() != null && CollectionProperties.isCollectionProperty( propertyName ) ) {
			// propertyName refers to something like collection.size...
			return;
		}
		if ( propertySource.isComponentType() ) {
			// property name is a sub-path of a component...
			return;
		}

		Queryable persister = getQueryable();
		if ( persister != null ) {
			try {
				Queryable.Declarer propertyDeclarer = persister.getSubclassPropertyDeclarer( propertyName );
				if ( LOG.isTraceEnabled() ) {
					LOG.tracev( "Handling property dereference [{0} ({1}) -> {2} ({3})]",
							persister.getEntityName(), getClassAlias(), propertyName, propertyDeclarer );
				}
				if ( propertyDeclarer == Queryable.Declarer.SUBCLASS ) {
					dereferencedBySubclassProperty = true;
					includeSubclasses = true;
				}
				else if ( propertyDeclarer == Queryable.Declarer.SUPERCLASS ) {
					dereferencedBySuperclassProperty = true;
				}
			}
			catch( QueryException ignore ) {
				// ignore it; the incoming property could not be found so we
				// cannot be sure what to do here.  At the very least, the
				// safest is to simply not apply any dereference toggling...

			}
		}
	}

	public boolean isDereferencedBySuperclassProperty() {
		return dereferencedBySuperclassProperty;
	}

	public boolean isDereferencedBySubclassProperty() {
		return dereferencedBySubclassProperty;
	}


	// ParameterContainer impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private List<ParameterSpecification> embeddedParameters = new ArrayList<>(  );

	@Override
	public void addEmbeddedParameter(ParameterSpecification specification) {
		embeddedParameters.add( specification );
	}

	@Override
	public boolean hasEmbeddedParameters() {
		return !embeddedParameters.isEmpty();
	}

	@Override
	public ParameterSpecification[] getEmbeddedParameters() {
		final List<ParameterSpecification> parameterSpecification = getParameterSpecification();
		return parameterSpecification.toArray( new ParameterSpecification[ parameterSpecification.size() ] );
	}

	private List<ParameterSpecification> getParameterSpecification() {
		List<ParameterSpecification> parameterSpecifications =
			embeddedParameters.stream()
					.filter( o -> o instanceof  DynamicFilterParameterSpecification )
					.collect( Collectors.toList() );

		parameterSpecifications.addAll(
			embeddedParameters.stream()
					.filter( o -> ! (o instanceof  DynamicFilterParameterSpecification ) )
					.collect( Collectors.toList() ) );
		return parameterSpecifications;
	}

	public ParameterSpecification getIndexCollectionSelectorParamSpec() {
		return elementType.getIndexCollectionSelectorParamSpec();
	}

	public void setIndexCollectionSelectorParamSpec(ParameterSpecification indexCollectionSelectorParamSpec) {
		if ( indexCollectionSelectorParamSpec == null ) {
			if ( elementType.getIndexCollectionSelectorParamSpec() != null ) {
				embeddedParameters.remove( elementType.getIndexCollectionSelectorParamSpec() );
				elementType.setIndexCollectionSelectorParamSpec( null );
			}
		}
		else {
			elementType.setIndexCollectionSelectorParamSpec( indexCollectionSelectorParamSpec );
			addEmbeddedParameter( indexCollectionSelectorParamSpec );
		}
	}
}
