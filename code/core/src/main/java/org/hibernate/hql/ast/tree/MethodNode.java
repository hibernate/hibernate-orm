// $Id: MethodNode.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import java.util.Arrays;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.CollectionProperties;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ASTUtil;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a method call.
 *
 * @author josh Aug 16, 2004 7:59:42 AM
 */
public class MethodNode extends AbstractSelectExpression implements SelectExpression {

	private static final Log log = LogFactory.getLog( MethodNode.class );

	private String methodName;
	private FromElement fromElement;
	private String[] selectColumns;
	private SQLFunction function;
	private boolean inSelect;

	public void resolve(boolean inSelect) throws SemanticException {
		// Get the function name node.
		AST name = getFirstChild();
		initializeMethodNode( name, inSelect );
		AST exprList = name.getNextSibling();
		// If the expression list has exactly one expression, and the type of the expression is a collection
		// then this might be a collection function, such as index(c) or size(c).
		if ( ASTUtil.hasExactlyOneChild( exprList ) && isCollectionPropertyMethod() ) {
			collectionProperty( exprList.getFirstChild(), name );
		}
		else {
			dialectFunction( exprList );
		}
	}

	public SQLFunction getSQLFunction() {
		return function;
	}

	private void dialectFunction(AST exprList) {
		function = getSessionFactoryHelper().findSQLFunction( methodName );
		if ( function != null ) {
			AST firstChild = exprList != null ? exprList.getFirstChild() : null;
			Type functionReturnType = getSessionFactoryHelper()
					.findFunctionReturnType( methodName, firstChild );
			setDataType( functionReturnType );
		}
		//TODO:
		/*else {
			methodName = (String) getWalker().getTokenReplacements().get( methodName );
		}*/
	}

	public boolean isCollectionPropertyMethod() {
		return CollectionProperties.isAnyCollectionProperty( methodName );
	}

	public void initializeMethodNode(AST name, boolean inSelect) {
		name.setType( SqlTokenTypes.METHOD_NAME );
		String text = name.getText();
		methodName = text.toLowerCase();	// Use the lower case function name.
		this.inSelect = inSelect;			// Remember whether we're in a SELECT clause or not.
	}

	private String getMethodName() {
		return methodName;
	}

	private void collectionProperty(AST path, AST name) throws SemanticException {
		if ( path == null ) {
			throw new SemanticException( "Collection function " + name.getText() + " has no path!" );
		}

		SqlNode expr = ( SqlNode ) path;
		Type type = expr.getDataType();
		if ( log.isDebugEnabled() ) {
			log.debug( "collectionProperty() :  name=" + name + " type=" + type );
		}

		resolveCollectionProperty( expr );
	}

	public boolean isScalar() throws SemanticException {
		// Method expressions in a SELECT should always be considered scalar.
		return true;
	}

	public void resolveCollectionProperty(AST expr) throws SemanticException {
		String propertyName = CollectionProperties.getNormalizedPropertyName( getMethodName() );
		if ( expr instanceof FromReferenceNode ) {
			FromReferenceNode collectionNode = ( FromReferenceNode ) expr;
			// If this is 'elements' then create a new FROM element.
			if ( CollectionPropertyNames.COLLECTION_ELEMENTS.equals( propertyName ) ) {
				handleElements( collectionNode, propertyName );
			}
			else {
				// Not elements(x)
				fromElement = collectionNode.getFromElement();
				setDataType( fromElement.getPropertyType( propertyName, propertyName ) );
				selectColumns = fromElement.toColumns( fromElement.getTableAlias(), propertyName, inSelect );
			}
			if ( collectionNode instanceof DotNode ) {
				prepareAnyImplicitJoins( ( DotNode ) collectionNode );
			}
			if ( !inSelect ) {
				fromElement.setText( "" );
				fromElement.setUseWhereFragment( false );
			}
			prepareSelectColumns( selectColumns );
			setText( selectColumns[0] );
			setType( SqlTokenTypes.SQL_TOKEN );
		}
		else {
			throw new SemanticException( 
					"Unexpected expression " + expr + 
					" found for collection function " + propertyName 
				);
		}
	}

	private void prepareAnyImplicitJoins(DotNode dotNode) throws SemanticException {
		if ( dotNode.getLhs() instanceof DotNode ) {
			DotNode lhs = ( DotNode ) dotNode.getLhs();
			FromElement lhsOrigin = lhs.getFromElement();
			if ( lhsOrigin != null && "".equals( lhsOrigin.getText() ) ) {
				String lhsOriginText = lhsOrigin.getQueryable().getTableName() +
				        " " + lhsOrigin.getTableAlias();
				lhsOrigin.setText( lhsOriginText );
			}
			prepareAnyImplicitJoins( lhs );
		}
	}

	private void handleElements(FromReferenceNode collectionNode, String propertyName) {
		FromElement collectionFromElement = collectionNode.getFromElement();
		QueryableCollection queryableCollection = collectionFromElement.getQueryableCollection();

		String path = collectionNode.getPath() + "[]." + propertyName;
		log.debug( "Creating elements for " + path );

		fromElement = collectionFromElement;
		if ( !collectionFromElement.isCollectionOfValuesOrComponents() ) {
			getWalker().addQuerySpaces( queryableCollection.getElementPersister().getQuerySpaces() );
		}

		setDataType( queryableCollection.getElementType() );
		selectColumns = collectionFromElement.toColumns( fromElement.getTableAlias(), propertyName, inSelect );
	}

	public void setScalarColumnText(int i) throws SemanticException {
		if ( selectColumns == null ) { 	// Dialect function
			ColumnHelper.generateSingleScalarColumn( this, i );
		}
		else {	// Collection 'property function'
			ColumnHelper.generateScalarColumns( this, selectColumns, i );
		}
	}

	protected void prepareSelectColumns(String[] columns) {
		return;
	}

	public FromElement getFromElement() {
		return fromElement;
	}

	public String getDisplayText() {
		return "{" +
				"method=" + getMethodName() +
				",selectColumns=" + ( selectColumns == null ? 
						null : Arrays.asList( selectColumns ) ) +
				",fromElement=" + fromElement.getTableAlias() +
				"}";
	}
}
