/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.util.SessionFactoryHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents the [] operator and provides it's semantics.
 *
 * @author josh
 */
public class IndexNode extends FromReferenceNode {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IndexNode.class );

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		throw new UnsupportedOperationException( "An IndexNode cannot generate column text!" );
	}

	@Override
	public void prepareForDot(String propertyName) throws SemanticException {
		FromElement fromElement = getFromElement();
		if ( fromElement == null ) {
			throw new IllegalStateException( "No FROM element for index operator!" );
		}

		final QueryableCollection queryableCollection = fromElement.getQueryableCollection();
		if ( queryableCollection != null && !queryableCollection.isOneToMany() ) {
			final FromReferenceNode collectionNode = (FromReferenceNode) getFirstChild();
			final String path = collectionNode.getPath() + "[]." + propertyName;
			LOG.debugf( "Creating join for many-to-many elements for %s", path );
			final FromElementFactory factory = new FromElementFactory( fromElement.getFromClause(), fromElement, path );
			// This will add the new from element to the origin.
			final FromElement elementJoin = factory.createElementJoin( queryableCollection );
			setFromElement( elementJoin );
		}
	}

	@Override
	public void resolveIndex(AST parent) throws SemanticException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent, AST parentPredicate)
			throws SemanticException {
		if ( isResolved() ) {
			return;
		}
		FromReferenceNode collectionNode = (FromReferenceNode) getFirstChild();
		SessionFactoryHelper sessionFactoryHelper = getSessionFactoryHelper();
		collectionNode.resolveIndex( this );        // Fully resolve the map reference, create implicit joins.

		Type type = collectionNode.getDataType();
		if ( !type.isCollectionType() ) {
			throw new SemanticException( "The [] operator cannot be applied to type " + type.toString() );
		}
		String collectionRole = ( (CollectionType) type ).getRole();
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
			LOG.debugf( "No FROM element found for the elements of collection join path %s, created %s", path, elem );
		}
		else {
			LOG.debugf( "FROM element found for collection join path %s", path );
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
		catch (RecognitionException e) {
			throw new QueryException( e.getMessage(), e );
		}
		String selectorExpression = gen.getSQL();
		joinSequence.addCondition( collectionTableAlias + '.' + indexCols[0] + " = " + selectorExpression );
		List<ParameterSpecification> paramSpecs = gen.getCollectedParameters();
		if ( paramSpecs != null ) {
			switch ( paramSpecs.size() ) {
				case 0:
					// nothing to do
					break;
				case 1:
					ParameterSpecification paramSpec = paramSpecs.get( 0 );
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
		private final List<ParameterSpecification> paramSpecs;

		public AggregatedIndexCollectionSelectorParameterSpecifications(List<ParameterSpecification> paramSpecs) {
			this.paramSpecs = paramSpecs;
		}

		@Override
		public int bind(PreparedStatement statement, QueryParameters qp, SharedSessionContractImplementor session, int position)
				throws SQLException {
			int bindCount = 0;
			for ( ParameterSpecification paramSpec : paramSpecs ) {
				bindCount += paramSpec.bind( statement, qp, session, position + bindCount );
			}
			return bindCount;
		}

		@Override
		public Type getExpectedType() {
			return null;
		}

		@Override
		public void setExpectedType(Type expectedType) {
		}

		@Override
		public String renderDisplayInfo() {
			return "index-selector [" + collectDisplayInfo() + "]";
		}

		private String collectDisplayInfo() {
			StringBuilder buffer = new StringBuilder();
			for ( ParameterSpecification paramSpec : paramSpecs ) {
				buffer.append( ( paramSpec ).renderDisplayInfo() );
			}
			return buffer.toString();
		}
	}
}
