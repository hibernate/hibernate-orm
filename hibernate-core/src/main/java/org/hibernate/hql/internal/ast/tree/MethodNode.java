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
package org.hibernate.hql.internal.ast.tree;
import java.util.Arrays;

import antlr.SemanticException;
import antlr.collections.AST;
import org.jboss.logging.Logger;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.internal.CollectionProperties;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.TypeDiscriminatorMetadata;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

/**
 * Represents a method call.
 *
 * @author josh
 */
public class MethodNode extends AbstractSelectExpression implements FunctionNode {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, MethodNode.class.getName());

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
		if ( ASTUtil.hasExactlyOneChild( exprList ) ) {
			if ( "type".equals( methodName ) ) {
				typeDiscriminator( exprList.getFirstChild() );
				return;
			}
			if ( isCollectionPropertyMethod() ) {
				collectionProperty( exprList.getFirstChild(), name );
				return;
			}
		}

		dialectFunction( exprList );
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

	public SQLFunction getSQLFunction() {
		return function;
	}

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
		LOG.debugf( "collectionProperty() :  name=%s type=%s", name, type );

		resolveCollectionProperty( expr );
	}

	@Override
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
		LOG.debugf( "Creating elements for %s", path );

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
	}

	@Override
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
