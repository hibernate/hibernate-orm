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

import java.util.List;
import java.util.Iterator;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.QueryException;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.engine.JoinSequence;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.hql.ast.SqlGenerator;
import org.hibernate.hql.ast.util.SessionFactoryHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.collections.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the [] operator and provides it's semantics.
 *
 * @author josh
 */
public class IndexNode extends FromReferenceNode {

	private static final Logger log = LoggerFactory.getLogger( IndexNode.class );

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

		// The 'from element' that represents the elements of the collection.
		setFromElement( fromElement );

		// Add the condition to the join sequence that qualifies the indexed element.
		AST selector = collectionNode.getNextSibling();
		if ( selector == null ) {
			throw new QueryException( "No index value!" );
		}

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
			gen.simpleExpr( selector ); //TODO: used to be exprNoParens! was this needed?
		}
		catch ( RecognitionException e ) {
			throw new QueryException( e.getMessage(), e );
		}
		String selectorExpression = gen.getSQL();
		joinSequence.addCondition( collectionTableAlias + '.' + indexCols[0] + " = " + selectorExpression );
		List paramSpecs = gen.getCollectedParameters();
		if ( paramSpecs != null ) {
			switch ( paramSpecs.size() ) {
				case 0 :
					// nothing to do
					break;
				case 1 :
					ParameterSpecification paramSpec = ( ParameterSpecification ) paramSpecs.get( 0 );
					paramSpec.setExpectedType( queryableCollection.getIndexType() );
					fromElement.setIndexCollectionSelectorParamSpec( paramSpec );
					break;
				default:
					fromElement.setIndexCollectionSelectorParamSpec(
							new AggregatedIndexCollectionSelectorParameterSpecifications( paramSpecs )
					);
					break;
			}
		}

		// Now, set the text for this node.  It should be the element columns.
		String[] elementColumns = queryableCollection.getElementColumnNames( elementTable );
		setText( elementColumns[0] );
		setResolved();
	}

	/**
	 * In the (rare?) case where the index selector contains multiple parameters...
	 */
	private static class AggregatedIndexCollectionSelectorParameterSpecifications implements ParameterSpecification {
		private final List paramSpecs;

		public AggregatedIndexCollectionSelectorParameterSpecifications(List paramSpecs) {
			this.paramSpecs = paramSpecs;
		}

		public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position)
		throws SQLException {
			int bindCount = 0;
			Iterator itr = paramSpecs.iterator();
			while ( itr.hasNext() ) {
				final ParameterSpecification paramSpec = ( ParameterSpecification ) itr.next();
				bindCount += paramSpec.bind( statement, qp, session, position + bindCount );
			}
			return bindCount;
		}

		public Type getExpectedType() {
			return null;
		}

		public void setExpectedType(Type expectedType) {
		}

		public String renderDisplayInfo() {
			return "index-selector [" + collectDisplayInfo() + "]" ;
		}

		private String collectDisplayInfo() {
			StringBuffer buffer = new StringBuffer();
			Iterator itr = paramSpecs.iterator();
			while ( itr.hasNext() ) {
				buffer.append( ( ( ParameterSpecification ) itr.next() ).renderDisplayInfo() );
			}
			return buffer.toString();
		}
	}
}
