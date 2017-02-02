/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;
import java.util.LinkedList;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.internal.CollectionSubqueryFactory;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Parses an expression of the form foo.bar.baz and builds up an expression
 * involving two less table joins than there are path components.
 */
public class PathExpressionParser implements Parser {

	//TODO: this class does too many things! we need a different
	//kind of path expression parser for each of the diffferent
	//ways in which path expressions can occur

	//We should actually rework this class to not implement Parser
	//and just process path expressions in the most convenient way.

	//The class is now way to complex!

	private int dotcount;
	private String currentName;
	private String currentProperty;
	private String oneToOneOwnerName;
	private AssociationType ownerAssociationType;
	private String[] columns;
	private String collectionName;
	private String collectionOwnerName;
	private String collectionRole;
	private final StringBuilder componentPath = new StringBuilder();
	private Type type;
	private final StringBuilder path = new StringBuilder();
	private boolean ignoreInitialJoin;
	private boolean continuation;
	private JoinType joinType = JoinType.INNER_JOIN; //default mode
	private boolean useThetaStyleJoin = true;
	private PropertyMapping currentPropertyMapping;
	private JoinSequence joinSequence;

	private boolean expectingCollectionIndex;
	private LinkedList collectionElements = new LinkedList();

	void setJoinType(JoinType joinType) {
		this.joinType = joinType;
	}

	void setUseThetaStyleJoin(boolean useThetaStyleJoin) {
		this.useThetaStyleJoin = useThetaStyleJoin;
	}

	private void addJoin(String name, AssociationType joinableType) throws QueryException {
		try {
			joinSequence.addJoin( joinableType, name, joinType, currentColumns() );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}

	private void addJoin(String name, AssociationType joinableType, String[] foreignKeyColumns) throws QueryException {
		try {
			joinSequence.addJoin( joinableType, name, joinType, foreignKeyColumns );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}

	String continueFromManyToMany(String entityName, String[] joinColumns, QueryTranslatorImpl q) throws QueryException {
		start( q );
		continuation = true;
		currentName = q.createNameFor( entityName );
		q.addType( currentName, entityName );
		Queryable classPersister = q.getEntityPersister( entityName );
		//QueryJoinFragment join = q.createJoinFragment(useThetaStyleJoin);
		addJoin( currentName, q.getFactory().getTypeResolver().getTypeFactory().manyToOne( entityName ), joinColumns );
		currentPropertyMapping = classPersister;
		return currentName;
	}

	public void ignoreInitialJoin() {
		ignoreInitialJoin = true;
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		if ( token != null ) {
			path.append( token );
		}

		String alias = q.getPathAlias( path.toString() );
		if ( alias != null ) {
			reset( q ); //reset the dotcount (but not the path)
			currentName = alias; //afterQuery reset!
			currentPropertyMapping = q.getPropertyMapping( currentName );
			if ( !ignoreInitialJoin ) {
				JoinSequence ojf = q.getPathJoin( path.toString() );
				try {
					joinSequence.addCondition( ojf.toJoinFragment( q.getEnabledFilters(), true ).toWhereFragmentString() ); //afterQuery reset!
				}
				catch ( MappingException me ) {
					throw new QueryException( me );
				}
				// we don't need to worry about any condition in the ON clause
				// here (toFromFragmentString), since anything in the ON condition
				// is already applied to the whole query
			}
		}
		else if ( ".".equals( token ) ) {
			dotcount++;
		}
		else {
			if ( dotcount == 0 ) {
				if ( !continuation ) {
					if ( !q.isName( token ) ) {
						throw new QueryException( "undefined alias: " + token );
					}
					currentName = token;
					currentPropertyMapping = q.getPropertyMapping( currentName );
				}
			}
			else if ( dotcount == 1 ) {
				if ( currentName != null ) {
					currentProperty = token;
				}
				else if ( collectionName != null ) {
					//processCollectionProperty(token, q.getCollectionPersister(collectionRole), collectionName);
					continuation = false;
				}
				else {
					throw new QueryException( "unexpected" );
				}
			}
			else { // dotcount>=2

				// Do the corresponding RHS
				Type propertyType = getPropertyType();

				if ( propertyType == null ) {
					throw new QueryException( "unresolved property: " + path );
				}

				if ( propertyType.isComponentType() ) {
					dereferenceComponent( token );
				}
				else if ( propertyType.isEntityType() ) {
					if ( !isCollectionValued() ) {
						dereferenceEntity( token, ( EntityType ) propertyType, q );
					}
				}
				else if ( propertyType.isCollectionType() ) {
					dereferenceCollection( token, ( ( CollectionType ) propertyType ).getRole(), q );

				}
				else {
					if ( token != null ) {
						throw new QueryException( "dereferenced: " + path );
					}
				}

			}
		}
	}

	private void dereferenceEntity(String propertyName, EntityType propertyType, QueryTranslatorImpl q)
			throws QueryException {
		//NOTE: we avoid joining to the next table if the named property is just the foreign key value

		//if its "id"
		boolean isIdShortcut = EntityPersister.ENTITY_ID.equals( propertyName )
				&& propertyType.isReferenceToPrimaryKey();

		//or its the id property name
		final String idPropertyName;
		try {
			idPropertyName = propertyType.getIdentifierOrUniqueKeyPropertyName( q.getFactory() );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
		boolean isNamedIdPropertyShortcut = idPropertyName != null
				&& idPropertyName.equals( propertyName )
				&& propertyType.isReferenceToPrimaryKey();


		if ( isIdShortcut || isNamedIdPropertyShortcut ) {
			// special shortcut for id properties, skip the join!
			// this must only occur at the _end_ of a path expression
			if ( componentPath.length() > 0 ) {
				componentPath.append( '.' );
			}
			componentPath.append( propertyName );
		}
		else {
			String entityClass = propertyType.getAssociatedEntityName();
			String name = q.createNameFor( entityClass );
			q.addType( name, entityClass );
			addJoin( name, propertyType );
			if ( propertyType.isOneToOne() ) {
				oneToOneOwnerName = currentName;
			}
			ownerAssociationType = propertyType;
			currentName = name;
			currentProperty = propertyName;
			q.addPathAliasAndJoin( path.substring( 0, path.toString().lastIndexOf( '.' ) ), name, joinSequence.copy() );
			componentPath.setLength( 0 );
			currentPropertyMapping = q.getEntityPersister( entityClass );
		}
	}

	private void dereferenceComponent(String propertyName) {
		if ( propertyName != null ) {
			if ( componentPath.length() > 0 ) {
				componentPath.append( '.' );
			}
			componentPath.append( propertyName );
		}
	}

	private void dereferenceCollection(String propertyName, String role, QueryTranslatorImpl q) throws QueryException {
		collectionRole = role;
		QueryableCollection collPersister = q.getCollectionPersister( role );
		String name = q.createNameForCollection( role );
		addJoin( name, collPersister.getCollectionType() );
		//if ( collPersister.hasWhere() ) join.addCondition( collPersister.getSQLWhereString(name) );
		collectionName = name;
		collectionOwnerName = currentName;
		currentName = name;
		currentProperty = propertyName;
		componentPath.setLength( 0 );
		currentPropertyMapping = new CollectionPropertyMapping( collPersister );
	}

	private String getPropertyPath() {
		if ( currentProperty == null ) {
			return EntityPersister.ENTITY_ID;
		}
		else {
			if ( componentPath.length() > 0 ) {
				return currentProperty + '.' + componentPath.toString();
			}
			else {
				return currentProperty;
			}
		}
	}

	private PropertyMapping getPropertyMapping() {
		return currentPropertyMapping;
	}

	private void setType() throws QueryException {
		if ( currentProperty == null ) {
			type = getPropertyMapping().getType();
		}
		else {
			type = getPropertyType();
		}
	}

	protected Type getPropertyType() throws QueryException {
		String propertyPath = getPropertyPath();
		Type propertyType = getPropertyMapping().toType( propertyPath );
		if ( propertyType == null ) {
			throw new QueryException( "could not resolve property type: " + propertyPath );
		}
		return propertyType;
	}

	protected String[] currentColumns() throws QueryException {
		String propertyPath = getPropertyPath();
		String[] propertyColumns = getPropertyMapping().toColumns( currentName, propertyPath );
		if ( propertyColumns == null ) {
			throw new QueryException( "could not resolve property columns: " + propertyPath );
		}
		return propertyColumns;
	}

	private void reset(QueryTranslatorImpl q) {
		//join = q.createJoinFragment(useThetaStyleJoin);
		dotcount = 0;
		currentName = null;
		currentProperty = null;
		collectionName = null;
		collectionRole = null;
		componentPath.setLength( 0 );
		type = null;
		collectionName = null;
		columns = null;
		expectingCollectionIndex = false;
		continuation = false;
		currentPropertyMapping = null;
	}

	public void start(QueryTranslatorImpl q) {
		if ( !continuation ) {
			reset( q );
			path.setLength( 0 );
			joinSequence = new JoinSequence( q.getFactory() ).setUseThetaStyle( useThetaStyleJoin );
		}
	}

	public void end(QueryTranslatorImpl q) throws QueryException {
		ignoreInitialJoin = false;

		Type propertyType = getPropertyType();
		if ( propertyType != null && propertyType.isCollectionType() ) {
			collectionRole = ( ( CollectionType ) propertyType ).getRole();
			collectionName = q.createNameForCollection( collectionRole );
			prepareForIndex( q );
		}
		else {
			columns = currentColumns();
			setType();
		}

		//important!!
		continuation = false;

	}

	private void prepareForIndex(QueryTranslatorImpl q) throws QueryException {

		QueryableCollection collPersister = q.getCollectionPersister( collectionRole );

		if ( !collPersister.hasIndex() ) {
			throw new QueryException( "unindexed collection beforeQuery []: " + path );
		}

		String[] indexCols = collPersister.getIndexColumnNames();
		if ( indexCols.length != 1 ) {
			throw new QueryException( "composite-index appears in []: " + path );
		}
		//String[] keyCols = collPersister.getKeyColumnNames();

		JoinSequence fromJoins = new JoinSequence( q.getFactory() )
				.setUseThetaStyle( useThetaStyleJoin )
				.setRoot( collPersister, collectionName )
				.setNext( joinSequence.copy() );

		if ( !continuation ) {
			addJoin( collectionName, collPersister.getCollectionType() );
		}

		joinSequence.addCondition( collectionName + '.' + indexCols[0] + " = " ); //TODO: get SQL rendering out of here

		CollectionElement elem = new CollectionElement();
		elem.elementColumns = collPersister.getElementColumnNames(collectionName);
		elem.elementType = collPersister.getElementType();
		elem.isOneToMany = collPersister.isOneToMany();
		elem.alias = collectionName;
		elem.joinSequence = joinSequence;
		collectionElements.addLast( elem );
		setExpectingCollectionIndex();

		q.addCollection( collectionName, collectionRole );
		q.addFromJoinOnly( collectionName, fromJoins );
	}

	static final class CollectionElement {
		Type elementType;
		boolean isOneToMany;
		String alias;
		String[] elementColumns;
		JoinSequence joinSequence;
		StringBuilder indexValue = new StringBuilder();
	}

	public CollectionElement lastCollectionElement() {
		return ( CollectionElement ) collectionElements.removeLast();
	}

	public void setLastCollectionElementIndexValue(String value) {
		( ( CollectionElement ) collectionElements.getLast() ).indexValue.append( value );
	}

	public boolean isExpectingCollectionIndex() {
		return expectingCollectionIndex;
	}

	protected void setExpectingCollectionIndex() throws QueryException {
		expectingCollectionIndex = true;
	}

	public JoinSequence getWhereJoin() {
		return joinSequence;
	}

	public String getWhereColumn() throws QueryException {
		if ( columns.length != 1 ) {
			throw new QueryException( "path expression ends in a composite value: " + path );
		}
		return columns[0];
	}

	public String[] getWhereColumns() {
		return columns;
	}

	public Type getWhereColumnType() {
		return type;
	}

	public String getName() {
		return currentName == null ? collectionName : currentName;
	}


	public String getCollectionSubquery(Map enabledFilters) throws QueryException {
		return CollectionSubqueryFactory.createCollectionSubquery( joinSequence, enabledFilters, currentColumns() );
	}

	public boolean isCollectionValued() throws QueryException {
		//TODO: is there a better way?
		return collectionName != null && !getPropertyType().isCollectionType();
	}

	public void addAssociation(QueryTranslatorImpl q) throws QueryException {
		q.addJoin( getName(), joinSequence );
	}

	public String addFromAssociation(QueryTranslatorImpl q) throws QueryException {
		if ( isCollectionValued() ) {
			return addFromCollection( q );
		}
		else {
			q.addFrom( currentName, joinSequence );
			return currentName;
		}
	}

	public String addFromCollection(QueryTranslatorImpl q) throws QueryException {
		Type collectionElementType = getPropertyType();

		if ( collectionElementType == null ) {
			throw new QueryException( "must specify 'elements' for collection valued property in from clause: " + path );
		}

		if ( collectionElementType.isEntityType() ) {
			// an association
			QueryableCollection collectionPersister = q.getCollectionPersister( collectionRole );
			Queryable entityPersister = ( Queryable ) collectionPersister.getElementPersister();
			String clazz = entityPersister.getEntityName();

			final String elementName;
			if ( collectionPersister.isOneToMany() ) {
				elementName = collectionName;
				//allow index() function:
				q.decoratePropertyMapping( elementName, collectionPersister );
			}
			else { //many-to-many
				q.addCollection( collectionName, collectionRole );
				elementName = q.createNameFor( clazz );
				addJoin( elementName, ( AssociationType ) collectionElementType );
			}
			q.addFrom( elementName, clazz, joinSequence );
			currentPropertyMapping = new CollectionPropertyMapping( collectionPersister );
			return elementName;
		}
		else {
			// collections of values
			q.addFromCollection( collectionName, collectionRole, joinSequence );
			return collectionName;
		}

	}

	String getCollectionName() {
		return collectionName;
	}

	String getCollectionRole() {
		return collectionRole;
	}

	String getCollectionOwnerName() {
		return collectionOwnerName;
	}

	String getOneToOneOwnerName() {
		return oneToOneOwnerName;
	}

	AssociationType getOwnerAssociationType() {
		return ownerAssociationType;
	}

	String getCurrentProperty() {
		return currentProperty;
	}

	String getCurrentName() {
		return currentName;
	}

	public void fetch(QueryTranslatorImpl q, String entityName) throws QueryException {
		if ( isCollectionValued() ) {
			q.setCollectionToFetch( getCollectionRole(), getCollectionName(), getCollectionOwnerName(), entityName );
		}
		else {
			q.addEntityToFetch( entityName, getOneToOneOwnerName(), getOwnerAssociationType() );
		}
	}
}
