/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.hql.ast.tree;

import java.util.Map;

import antlr.SemanticException;
import antlr.collections.AST;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.util.StringHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMapComponentNode extends FromReferenceNode implements HqlSqlTokenTypes {
	private String[] columns;

	public FromReferenceNode getMapReference() {
		return ( FromReferenceNode ) getFirstChild();
	}

	public String[] getColumns() {
		return columns;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateScalarColumns( this, getColumns(), i );
	}

	public void resolve(
			boolean generateJoin,
			boolean implicitJoin,
			String classAlias,
			AST parent) throws SemanticException {
		if ( parent != null ) {
			throw attemptedDereference();
		}

		FromReferenceNode mapReference = getMapReference();
		mapReference.resolve( true, true );
		if ( mapReference.getDataType().isCollectionType() ) {
			CollectionType collectionType = (CollectionType) mapReference.getDataType();
			if ( Map.class.isAssignableFrom( collectionType.getReturnedClass() ) ) {
				FromElement sourceFromElement = mapReference.getFromElement();
				setFromElement( sourceFromElement );
				setDataType( resolveType( sourceFromElement.getQueryableCollection() ) );
				this.columns = resolveColumns( sourceFromElement.getQueryableCollection() );
				initText( this.columns );
				setFirstChild( null );
				return;
			}
		}

		throw nonMap();
	}

	private void initText(String[] columns) {
		String text = StringHelper.join( ", ", columns );
		if ( columns.length > 1 && getWalker().isComparativeExpressionClause() ) {
			text = "(" + text + ")";
		}
		setText( text );
	}

	protected abstract String expressionDescription();
	protected abstract String[] resolveColumns(QueryableCollection collectionPersister);
	protected abstract Type resolveType(QueryableCollection collectionPersister);

	protected SemanticException attemptedDereference() {
		return new SemanticException( expressionDescription() + " expression cannot be further de-referenced" );
	}

	protected SemanticException nonMap() {
		return new SemanticException( expressionDescription() + " expression did not reference map property" );
	}

	public void resolveIndex(AST parent) throws SemanticException {
		throw new UnsupportedOperationException( expressionDescription() + " expression cannot be the source for an index operation" );
	}
}
