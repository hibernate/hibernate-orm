// $Id: IndexNode.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.ast.SqlGenerator;
import org.hibernate.hql.ast.util.SessionFactoryHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.collections.AST;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents the [] operator and provides it's semantics.
 *
 * @author josh Aug 14, 2004 7:07:10 AM
 */
public class IndexNode extends FromReferenceNode {

	private static final Log log = LogFactory.getLog( IndexNode.class );

	public void setScalarColumnText(int i) throws SemanticException {
		throw new UnsupportedOperationException( "An IndexNode cannot generate column text!" );
	}

	public void prepareForDot(String propertyName) throws SemanticException {
		FromElement fromElement = getFromElement();
		if ( fromElement == null ) {
			throw new IllegalStateException( "No FROM element for index operator!" );
		}
		QueryableCollection queryableCollection = fromElement.getQueryableCollection();
		if ( queryableCollection != null && !queryableCollection.isOneToMany() ) {

			FromReferenceNode collectionNode = ( FromReferenceNode ) getFirstChild();
			String path = collectionNode.getPath() + "[]." + propertyName;
			if ( log.isDebugEnabled() ) {
				log.debug( "Creating join for many-to-many elements for " + path );
			}
			FromElementFactory factory = new FromElementFactory( fromElement.getFromClause(), fromElement, path );
			// This will add the new from element to the origin.
			FromElement elementJoin = factory.createElementJoin( queryableCollection );
			setFromElement( elementJoin );
		}
	}

	public void resolveIndex(AST parent) throws SemanticException {
		throw new UnsupportedOperationException();
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) 
	throws SemanticException {
		if ( isResolved() ) {
			return;
		}
		FromReferenceNode collectionNode = ( FromReferenceNode ) getFirstChild();
		SessionFactoryHelper sessionFactoryHelper = getSessionFactoryHelper();
		collectionNode.resolveIndex( this );		// Fully resolve the map reference, create implicit joins.

		Type type = collectionNode.getDataType();
		if ( !type.isCollectionType() ) {
			throw new SemanticException( "The [] operator cannot be applied to type " + type.toString() );
		}
		String collectionRole = ( ( CollectionType ) type ).getRole();
		QueryableCollection queryableCollection = sessionFactoryHelper.requireQueryableCollection( collectionRole );
		if ( !queryableCollection.hasIndex() ) {
			throw new QueryException( "unindexed fromElement before []: " + collectionNode.getPath() );
		}

		// Generate the inner join -- The elements need to be joined to the collection they are in.
		FromElement fromElement = collectionNode.getFromElement();
		String elementTable = fromElement.getTableAlias();
		FromClause fromClause = fromElement.getFromClause();
		String path = collectionNode.getPath();

		FromElement elem = fromClause.findCollectionJoin( path );
		if ( elem == null ) {
			FromElementFactory factory = new FromElementFactory( fromClause, fromElement, path );
			elem = factory.createCollectionElementsJoin( queryableCollection, elementTable );
			if ( log.isDebugEnabled() ) {
				log.debug( "No FROM element found for the elements of collection join path " + path
						+ ", created " + elem );
			}
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debug( "FROM element found for collection join path " + path );
			}
		}

		// Add the condition to the join sequence that qualifies the indexed element.
		AST index = collectionNode.getNextSibling();	// The index should be a constant, which will have been processed already.
		if ( index == null ) {
			throw new QueryException( "No index value!" );
		}

		setFromElement( fromElement );							// The 'from element' that represents the elements of the collection.

		// Sometimes use the element table alias, sometimes use the... umm... collection table alias (many to many)
		String collectionTableAlias = elementTable;
		if ( elem.getCollectionTableAlias() != null ) {
			collectionTableAlias = elem.getCollectionTableAlias();
		}

		// TODO: get SQL rendering out of here, create an AST for the join expressions.
		// Use the SQL generator grammar to generate the SQL text for the index expression.
		JoinSequence joinSequence = fromElement.getJoinSequence();
		String[] indexCols = queryableCollection.getIndexColumnNames();
		if ( indexCols.length != 1 ) {
			throw new QueryException( "composite-index appears in []: " + collectionNode.getPath() );
		}
		SqlGenerator gen = new SqlGenerator( getSessionFactoryHelper().getFactory() );
		try {
			gen.simpleExpr( index ); //TODO: used to be exprNoParens! was this needed?
		}
		catch ( RecognitionException e ) {
			throw new QueryException( e.getMessage(), e );
		}
		String expression = gen.getSQL();
		joinSequence.addCondition( collectionTableAlias + '.' + indexCols[0] + " = " + expression );

		// Now, set the text for this node.  It should be the element columns.
		String[] elementColumns = queryableCollection.getElementColumnNames( elementTable );
		setText( elementColumns[0] );
		setResolved();
	}


}
