/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Arrays;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.internal.CollectionProperties;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.TypeDiscriminatorMetadata;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import antlr.SemanticException;
import antlr.collections.AST;
import java.util.Locale;

/**
 * Represents a method call.
 *
 * @author josh
 */
public class MethodNode extends AbstractSelectExpression implements FunctionNode {
	private static final Logger LOG = CoreLogging.logger( MethodNode.class );

	private String methodName;
	private FromElement fromElement;
	private String[] selectColumns;
	private SQLFunction function;
	private boolean inSelect;

	@Override
	public boolean isScalar() throws SemanticException {
		// Method expressions in a SELECT should always be considered scalar.
		return true;
	}

	@Override
	public SQLFunction getSQLFunction() {
		return function;
	}

	@Override
	public Type getFirstArgumentType() {
		AST argument = getFirstChild();
		while ( argument != null ) {
			if ( argument instanceof SqlNode ) {
				final Type type = ( (SqlNode) argument ).getDataType();
				if ( type != null ) {
					return type;
				}
				argument = argument.getNextSibling();
			}
		}
		return null;
	}

	public void resolve(boolean inSelect) throws SemanticException {
		// Get the function name node.
		AST nameNode = getFirstChild();
		AST exprListNode = nameNode.getNextSibling();

		initializeMethodNode( nameNode, inSelect );

		// If the expression list has exactly one expression, and the type of the expression is a collection
		// then this might be a collection function, such as index(c) or size(c).
		if ( ASTUtil.hasExactlyOneChild( exprListNode ) ) {
			if ( "type".equals( methodName ) ) {
				typeDiscriminator( exprListNode.getFirstChild() );
				return;
			}
			if ( isCollectionPropertyMethod() ) {
				collectionProperty( exprListNode.getFirstChild(), nameNode );
				return;
			}
		}

		dialectFunction( exprListNode );
	}

	public void initializeMethodNode(AST name, boolean inSelect) {
		name.setType( SqlTokenTypes.METHOD_NAME );
		String text = name.getText();
		// Use the lower case function name.
		methodName = text.toLowerCase(Locale.ROOT);
		// Remember whether we're in a SELECT clause or not.
		this.inSelect = inSelect;
	}

	private void typeDiscriminator(AST path) throws SemanticException {
		if ( path == null ) {
			throw new SemanticException( "type() discriminator reference has no path!" );
		}

		FromReferenceNode pathAsFromReferenceNode = (FromReferenceNode) path;
		FromElement fromElement = pathAsFromReferenceNode.getFromElement();
		TypeDiscriminatorMetadata typeDiscriminatorMetadata = fromElement.getTypeDiscriminatorMetadata();

		setDataType( typeDiscriminatorMetadata.getResolutionType() );
		setText( typeDiscriminatorMetadata.getSqlFragment() );
		setType( SqlTokenTypes.SQL_TOKEN );
	}

	private void dialectFunction(AST exprList) {
		function = getSessionFactoryHelper().findSQLFunction( methodName );
		if ( function != null ) {
			AST firstChild = exprList != null ? exprList.getFirstChild() : null;
			Type functionReturnType = getSessionFactoryHelper()
					.findFunctionReturnType( methodName, function, firstChild );
			setDataType( functionReturnType );
		}
	}

	public boolean isCollectionPropertyMethod() {
		return CollectionProperties.isAnyCollectionProperty( methodName );
	}

	private void collectionProperty(AST path, AST name) throws SemanticException {
		if ( path == null ) {
			throw new SemanticException( "Collection function " + name.getText() + " has no path!" );
		}

		SqlNode expr = (SqlNode) path;
		Type type = expr.getDataType();
		LOG.debugf( "collectionProperty() :  name=%s type=%s", name, type );

		resolveCollectionProperty( expr );
	}

	protected void resolveCollectionProperty(AST expr) throws SemanticException {
		String propertyName = CollectionProperties.getNormalizedPropertyName( methodName );
		if ( expr instanceof FromReferenceNode ) {
			FromReferenceNode collectionNode = (FromReferenceNode) expr;
			// If this is 'elements' then create a new FROM element.
			if ( CollectionPropertyNames.COLLECTION_ELEMENTS.equals( propertyName ) ) {
				handleElements( collectionNode, propertyName );
			}
			else {
				// Not elements(x)
				fromElement = collectionNode.getFromElement();

				final CollectionPropertyReference cpr = fromElement.getCollectionPropertyReference( propertyName );
				setDataType( cpr.getType() );
				selectColumns = cpr.toColumns( fromElement.getTableAlias() );

//				setDataType( fromElement.getPropertyType( propertyName, propertyName ) );
				selectColumns = fromElement.toColumns( fromElement.getTableAlias(), propertyName, inSelect );
			}
			if ( collectionNode instanceof DotNode ) {
				prepareAnyImplicitJoins( (DotNode) collectionNode );
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
			DotNode lhs = (DotNode) dotNode.getLhs();
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
		LOG.debugf( "Creating elements for %s", path );

		fromElement = collectionFromElement;
		if ( !collectionFromElement.isCollectionOfValuesOrComponents() ) {
			getWalker().addQuerySpaces( queryableCollection.getElementPersister().getQuerySpaces() );
		}

		setDataType( queryableCollection.getElementType() );
		selectColumns = collectionFromElement.toColumns( fromElement.getTableAlias(), propertyName, inSelect );
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		if ( selectColumns == null ) {    // Dialect function
			ColumnHelper.generateSingleScalarColumn( this, i );
		}
		else {    // Collection 'property function'
			ColumnHelper.generateScalarColumns( this, selectColumns, i );
		}
	}

	protected void prepareSelectColumns(String[] columns) {
	}

	@Override
	public FromElement getFromElement() {
		return fromElement;
	}

	public String getDisplayText() {
		return "{" +
				"method=" + methodName +
				",selectColumns=" + ( selectColumns == null ?
				null : Arrays.asList( selectColumns ) ) +
				",fromElement=" + fromElement.getTableAlias() +
				"}";
	}
}
