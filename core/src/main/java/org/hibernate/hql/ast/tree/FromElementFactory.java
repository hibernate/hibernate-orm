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
package org.hibernate.hql.ast.tree;

import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ASTUtil;
import org.hibernate.hql.ast.util.AliasGenerator;
import org.hibernate.hql.ast.util.PathHelper;
import org.hibernate.hql.ast.util.SessionFactoryHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.ComponentType;
import org.hibernate.util.StringHelper;

import antlr.ASTFactory;
import antlr.SemanticException;
import antlr.collections.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the creation of FromElements and JoinSequences.
 *
 * @author josh
 */
public class FromElementFactory implements SqlTokenTypes {

	private static final Logger log = LoggerFactory.getLogger( FromElementFactory.class );

	private FromClause fromClause;
	private FromElement origin;
	private String path;

	private String classAlias;
	private String[] columns;
	private boolean implied;
	private boolean inElementsFunction;
	private boolean collection;
	private QueryableCollection queryableCollection;
	private CollectionType collectionType;

	/**
	 * Creates entity from elements.
	 */
	public FromElementFactory(FromClause fromClause, FromElement origin, String path) {
		this.fromClause = fromClause;
		this.origin = origin;
		this.path = path;
		collection = false;
	}

	/**
	 * Creates collection from elements.
	 */
	public FromElementFactory(
	        FromClause fromClause,
	        FromElement origin,
	        String path,
	        String classAlias,
	        String[] columns,
	        boolean implied) {
		this( fromClause, origin, path );
		this.classAlias = classAlias;
		this.columns = columns;
		this.implied = implied;
		collection = true;
	}

	FromElement addFromElement() throws SemanticException {
		FromClause parentFromClause = fromClause.getParentFromClause();
		if ( parentFromClause != null ) {
			// Look up class name using the first identifier in the path.
			String pathAlias = PathHelper.getAlias( path );
			FromElement parentFromElement = parentFromClause.getFromElement( pathAlias );
			if ( parentFromElement != null ) {
				return createFromElementInSubselect( path, pathAlias, parentFromElement, classAlias );
			}
		}

		EntityPersister entityPersister = fromClause.getSessionFactoryHelper().requireClassPersister( path );

		FromElement elem = createAndAddFromElement( path,
				classAlias,
				entityPersister,
				( EntityType ) ( ( Queryable ) entityPersister ).getType(),
				null );

		// Add to the query spaces.
		fromClause.getWalker().addQuerySpaces( entityPersister.getQuerySpaces() );

		return elem;
	}

	private FromElement createFromElementInSubselect(
	        String path,
	        String pathAlias,
	        FromElement parentFromElement,
	        String classAlias) throws SemanticException {
		if ( log.isDebugEnabled() ) {
			log.debug( "createFromElementInSubselect() : path = " + path );
		}
		// Create an DotNode AST for the path and resolve it.
		FromElement fromElement = evaluateFromElementPath( path, classAlias );
		EntityPersister entityPersister = fromElement.getEntityPersister();

		// If the first identifier in the path referrs to the class alias (not the class name), then this
		// is a correlated subselect.  If it's a correlated sub-select, use the existing table alias.  Otherwise
		// generate a new one.
		String tableAlias = null;
		boolean correlatedSubselect = pathAlias.equals( parentFromElement.getClassAlias() );
		if ( correlatedSubselect ) {
			tableAlias = fromElement.getTableAlias();
		}
		else {
			tableAlias = null;
		}

		// If the from element isn't in the same clause, create a new from element.
		if ( fromElement.getFromClause() != fromClause ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "createFromElementInSubselect() : creating a new FROM element..." );
			}
			fromElement = createFromElement( entityPersister );
			initializeAndAddFromElement( fromElement,
					path,
					classAlias,
					entityPersister,
					( EntityType ) ( ( Queryable ) entityPersister ).getType(),
					tableAlias
			);
		}
		if ( log.isDebugEnabled() ) {
			log.debug( "createFromElementInSubselect() : " + path + " -> " + fromElement );
		}
		return fromElement;
	}

	private FromElement evaluateFromElementPath(String path, String classAlias) throws SemanticException {
		ASTFactory factory = fromClause.getASTFactory();
		FromReferenceNode pathNode = ( FromReferenceNode ) PathHelper.parsePath( path, factory );
		pathNode.recursiveResolve( FromReferenceNode.ROOT_LEVEL, // This is the root level node.
				false, // Generate an explicit from clause at the root.
				classAlias,
		        null
		);
		if ( pathNode.getImpliedJoin() != null ) {
			return pathNode.getImpliedJoin();
		}
		else {
			return pathNode.getFromElement();
		}
	}

	FromElement createCollectionElementsJoin(
	        QueryableCollection queryableCollection,
	        String collectionName) throws SemanticException {
		JoinSequence collectionJoinSequence = fromClause.getSessionFactoryHelper()
		        .createCollectionJoinSequence( queryableCollection, collectionName );
		this.queryableCollection = queryableCollection;
		return createCollectionJoin( collectionJoinSequence, null );
	}

	FromElement createCollection(
	        QueryableCollection queryableCollection,
	        String role,
	        int joinType,
	        boolean fetchFlag,
	        boolean indexed)
			throws SemanticException {
		if ( !collection ) {
			throw new IllegalStateException( "FromElementFactory not initialized for collections!" );
		}
		this.inElementsFunction = indexed;
		FromElement elem;
		this.queryableCollection = queryableCollection;
		collectionType = queryableCollection.getCollectionType();
		String roleAlias = fromClause.getAliasGenerator().createName( role );

		// Correlated subqueries create 'special' implied from nodes
		// because correlated subselects can't use an ANSI-style join
		boolean explicitSubqueryFromElement = fromClause.isSubQuery() && !implied;
		if ( explicitSubqueryFromElement ) {
			String pathRoot = StringHelper.root( path );
			FromElement origin = fromClause.getFromElement( pathRoot );
			if ( origin == null || origin.getFromClause() != fromClause ) {
				implied = true;
			}
		}

		// super-duper-classic-parser-regression-testing-mojo-magic...
		if ( explicitSubqueryFromElement && DotNode.useThetaStyleImplicitJoins ) {
			implied = true;
		}

		Type elementType = queryableCollection.getElementType();
		if ( elementType.isEntityType() ) { 			// A collection of entities...
			elem = createEntityAssociation( role, roleAlias, joinType );
		}
		else if ( elementType.isComponentType() ) {		// A collection of components...
			JoinSequence joinSequence = createJoinSequence( roleAlias, joinType );
			elem = createCollectionJoin( joinSequence, roleAlias );
		}
		else {											// A collection of scalar elements...
			JoinSequence joinSequence = createJoinSequence( roleAlias, joinType );
			elem = createCollectionJoin( joinSequence, roleAlias );
		}

		elem.setRole( role );
		elem.setQueryableCollection( queryableCollection );
		// Don't include sub-classes for implied collection joins or subquery joins.
		if ( implied ) {
			elem.setIncludeSubclasses( false );
		}

		if ( explicitSubqueryFromElement ) {
			elem.setInProjectionList( true );	// Treat explict from elements in sub-queries properly.
		}

		if ( fetchFlag ) {
			elem.setFetch( true );
		}
		return elem;
	}

	FromElement createEntityJoin(
	        String entityClass,
	        String tableAlias,
	        JoinSequence joinSequence,
	        boolean fetchFlag,
	        boolean inFrom,
	        EntityType type) throws SemanticException {
		FromElement elem = createJoin( entityClass, tableAlias, joinSequence, type, false );
		elem.setFetch( fetchFlag );
		EntityPersister entityPersister = elem.getEntityPersister();
		int numberOfTables = entityPersister.getQuerySpaces().length;
		if ( numberOfTables > 1 && implied && !elem.useFromFragment() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "createEntityJoin() : Implied multi-table entity join" );
			}
			elem.setUseFromFragment( true );
		}

		// If this is an implied join in a FROM clause, then use ANSI-style joining, and set the
		// flag on the FromElement that indicates that it was implied in the FROM clause itself.
		if ( implied && inFrom ) {
			joinSequence.setUseThetaStyle( false );
			elem.setUseFromFragment( true );
			elem.setImpliedInFromClause( true );
		}
		if ( elem.getWalker().isSubQuery() ) {
			// two conditions where we need to transform this to a theta-join syntax:
			//      1) 'elem' is the "root from-element" in correlated subqueries
			//      2) The DotNode.useThetaStyleImplicitJoins has been set to true
			//          and 'elem' represents an implicit join
			if ( elem.getFromClause() != elem.getOrigin().getFromClause() ||
//			        ( implied && DotNode.useThetaStyleImplicitJoins ) ) {
			        DotNode.useThetaStyleImplicitJoins ) {
				// the "root from-element" in correlated subqueries do need this piece
				elem.setType( FROM_FRAGMENT );
				joinSequence.setUseThetaStyle( true );
				elem.setUseFromFragment( false );
			}
		}

		return elem;
	}

	public FromElement createComponentJoin(ComponentType type) {
		// need to create a "place holder" from-element that can store the component/alias for this
		// 		component join
		return new ComponentJoin( fromClause, origin, classAlias, path, type );
	}

	FromElement createElementJoin(QueryableCollection queryableCollection) throws SemanticException {
		FromElement elem;

		implied = true; //TODO: always true for now, but not if we later decide to support elements() in the from clause
		inElementsFunction = true;
		Type elementType = queryableCollection.getElementType();
		if ( !elementType.isEntityType() ) {
			throw new IllegalArgumentException( "Cannot create element join for a collection of non-entities!" );
		}
		this.queryableCollection = queryableCollection;
		SessionFactoryHelper sfh = fromClause.getSessionFactoryHelper();
		FromElement destination = null;
		String tableAlias = null;
		EntityPersister entityPersister = queryableCollection.getElementPersister();
		tableAlias = fromClause.getAliasGenerator().createName( entityPersister.getEntityName() );
		String associatedEntityName = entityPersister.getEntityName();
		EntityPersister targetEntityPersister = sfh.requireClassPersister( associatedEntityName );
		// Create the FROM element for the target (the elements of the collection).
		destination = createAndAddFromElement( 
				associatedEntityName,
				classAlias,
				targetEntityPersister,
				( EntityType ) queryableCollection.getElementType(),
				tableAlias
			);
		// If the join is implied, then don't include sub-classes on the element.
		if ( implied ) {
			destination.setIncludeSubclasses( false );
		}
		fromClause.addCollectionJoinFromElementByPath( path, destination );
//		origin.addDestination(destination);
		// Add the query spaces.
		fromClause.getWalker().addQuerySpaces( entityPersister.getQuerySpaces() );

		CollectionType type = queryableCollection.getCollectionType();
		String role = type.getRole();
		String roleAlias = origin.getTableAlias();

		String[] targetColumns = sfh.getCollectionElementColumns( role, roleAlias );
		AssociationType elementAssociationType = sfh.getElementAssociationType( type );

		// Create the join element under the from element.
		int joinType = JoinFragment.INNER_JOIN;
		JoinSequence joinSequence = sfh.createJoinSequence( implied, elementAssociationType, tableAlias, joinType, targetColumns );
		elem = initializeJoin( path, destination, joinSequence, targetColumns, origin, false );
		elem.setUseFromFragment( true );	// The associated entity is implied, but it must be included in the FROM.
		elem.setCollectionTableAlias( roleAlias );	// The collection alias is the role.
		return elem;
	}

	private FromElement createCollectionJoin(JoinSequence collectionJoinSequence, String tableAlias) throws SemanticException {
		String text = queryableCollection.getTableName();
		AST ast = createFromElement( text );
		FromElement destination = ( FromElement ) ast;
		Type elementType = queryableCollection.getElementType();
		if ( elementType.isCollectionType() ) {
			throw new SemanticException( "Collections of collections are not supported!" );
		}
		destination.initializeCollection( fromClause, classAlias, tableAlias );
		destination.setType( JOIN_FRAGMENT );		// Tag this node as a JOIN.
		destination.setIncludeSubclasses( false );	// Don't include subclasses in the join.
		destination.setCollectionJoin( true );		// This is a clollection join.
		destination.setJoinSequence( collectionJoinSequence );
		destination.setOrigin( origin, false );
		destination.setCollectionTableAlias(tableAlias);
//		origin.addDestination( destination );
// This was the cause of HHH-242
//		origin.setType( FROM_FRAGMENT );			// Set the parent node type so that the AST is properly formed.
		origin.setText( "" );						// The destination node will have all the FROM text.
		origin.setCollectionJoin( true );			// The parent node is a collection join too (voodoo - see JoinProcessor)
		fromClause.addCollectionJoinFromElementByPath( path, destination );
		fromClause.getWalker().addQuerySpaces( queryableCollection.getCollectionSpaces() );
		return destination;
	}

	private FromElement createEntityAssociation(
	        String role,
	        String roleAlias,
	        int joinType) throws SemanticException {
		FromElement elem;
		Queryable entityPersister = ( Queryable ) queryableCollection.getElementPersister();
		String associatedEntityName = entityPersister.getEntityName();
		// Get the class name of the associated entity.
		if ( queryableCollection.isOneToMany() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "createEntityAssociation() : One to many - path = " + path + " role = " + role + " associatedEntityName = " + associatedEntityName );
			}
			JoinSequence joinSequence = createJoinSequence( roleAlias, joinType );

			elem = createJoin( associatedEntityName, roleAlias, joinSequence, ( EntityType ) queryableCollection.getElementType(), false );
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debug( "createManyToMany() : path = " + path + " role = " + role + " associatedEntityName = " + associatedEntityName );
			}
			elem = createManyToMany( role, associatedEntityName,
					roleAlias, entityPersister, ( EntityType ) queryableCollection.getElementType(), joinType );
			fromClause.getWalker().addQuerySpaces( queryableCollection.getCollectionSpaces() );
		}
		elem.setCollectionTableAlias( roleAlias );
		return elem;
	}

	private FromElement createJoin(
	        String entityClass,
	        String tableAlias,
	        JoinSequence joinSequence,
	        EntityType type,
	        boolean manyToMany) throws SemanticException {
		//  origin, path, implied, columns, classAlias,
		EntityPersister entityPersister = fromClause.getSessionFactoryHelper().requireClassPersister( entityClass );
		FromElement destination = createAndAddFromElement( entityClass,
				classAlias,
				entityPersister,
				type,
				tableAlias );
		return initializeJoin( path, destination, joinSequence, getColumns(), origin, manyToMany );
	}

	private FromElement createManyToMany(
	        String role,
	        String associatedEntityName,
	        String roleAlias,
	        Queryable entityPersister,
	        EntityType type,
	        int joinType) throws SemanticException {
		FromElement elem;
		SessionFactoryHelper sfh = fromClause.getSessionFactoryHelper();
		if ( inElementsFunction /*implied*/ ) {
			// For implied many-to-many, just add the end join.
			JoinSequence joinSequence = createJoinSequence( roleAlias, joinType );
			elem = createJoin( associatedEntityName, roleAlias, joinSequence, type, true );
		}
		else {
			// For an explicit many-to-many relationship, add a second join from the intermediate 
			// (many-to-many) table to the destination table.  Also, make sure that the from element's 
			// idea of the destination is the destination table.
			String tableAlias = fromClause.getAliasGenerator().createName( entityPersister.getEntityName() );
			String[] secondJoinColumns = sfh.getCollectionElementColumns( role, roleAlias );
			// Add the second join, the one that ends in the destination table.
			JoinSequence joinSequence = createJoinSequence( roleAlias, joinType );
			joinSequence.addJoin( sfh.getElementAssociationType( collectionType ), tableAlias, joinType, secondJoinColumns );
			elem = createJoin( associatedEntityName, tableAlias, joinSequence, type, false );
			elem.setUseFromFragment( true );
		}
		return elem;
	}

	private JoinSequence createJoinSequence(String roleAlias, int joinType) {
		SessionFactoryHelper sessionFactoryHelper = fromClause.getSessionFactoryHelper();
		String[] joinColumns = getColumns();
		if ( collectionType == null ) {
			throw new IllegalStateException( "collectionType is null!" );
		}
		return sessionFactoryHelper.createJoinSequence( implied, collectionType, roleAlias, joinType, joinColumns );
	}

	private FromElement createAndAddFromElement(
	        String className,
	        String classAlias,
	        EntityPersister entityPersister,
	        EntityType type,
	        String tableAlias) {
		if ( !( entityPersister instanceof Joinable ) ) {
			throw new IllegalArgumentException( "EntityPersister " + entityPersister + " does not implement Joinable!" );
		}
		FromElement element = createFromElement( entityPersister );
		initializeAndAddFromElement( element, className, classAlias, entityPersister, type, tableAlias );
		return element;
	}

	private void initializeAndAddFromElement(
	        FromElement element,
	        String className,
	        String classAlias,
	        EntityPersister entityPersister,
	        EntityType type,
	        String tableAlias) {
		if ( tableAlias == null ) {
			AliasGenerator aliasGenerator = fromClause.getAliasGenerator();
			tableAlias = aliasGenerator.createName( entityPersister.getEntityName() );
		}
		element.initializeEntity( fromClause, className, entityPersister, type, classAlias, tableAlias );
	}

	private FromElement createFromElement(EntityPersister entityPersister) {
		Joinable joinable = ( Joinable ) entityPersister;
		String text = joinable.getTableName();
		AST ast = createFromElement( text );
		FromElement element = ( FromElement ) ast;
		return element;
	}

	private AST createFromElement(String text) {
		AST ast = ASTUtil.create( fromClause.getASTFactory(),
				implied ? IMPLIED_FROM : FROM_FRAGMENT, // This causes the factory to instantiate the desired class.
				text );
		// Reset the node type, because the rest of the system is expecting FROM_FRAGMENT, all we wanted was
		// for the factory to create the right sub-class.  This might get reset again later on anyway to make the
		// SQL generation simpler.
		ast.setType( FROM_FRAGMENT );
		return ast;
	}

	private FromElement initializeJoin(
	        String path,
	        FromElement destination,
	        JoinSequence joinSequence,
	        String[] columns,
	        FromElement origin,
	        boolean manyToMany) {
		destination.setType( JOIN_FRAGMENT );
		destination.setJoinSequence( joinSequence );
		destination.setColumns( columns );
		destination.setOrigin( origin, manyToMany );
		fromClause.addJoinByPathMap( path, destination );
		return destination;
	}

	private String[] getColumns() {
		if ( columns == null ) {
			throw new IllegalStateException( "No foriegn key columns were supplied!" );
		}
		return columns;
	}
}
