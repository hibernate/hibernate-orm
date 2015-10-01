/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Map;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Basic support for KEY, VALUE and ENTRY based "qualified identification variables".
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMapComponentNode extends FromReferenceNode implements HqlSqlTokenTypes {
	private FromElement mapFromElement;
	private String[] columns;

	public FromReferenceNode getMapReference() {
		return (FromReferenceNode) getFirstChild();
	}

	public String[] getColumns() {
		return columns;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateScalarColumns( this, getColumns(), i );
	}

	@Override
	public void resolve(
			boolean generateJoin,
			boolean implicitJoin,
			String classAlias,
			AST parent) throws SemanticException {
		if ( parent != null ) {
			throw attemptedDereference();
		}

		final FromReferenceNode mapReference = getMapReference();
		mapReference.resolve( true, true );

		FromElement sourceFromElement = null;
		if ( isAliasRef( mapReference ) ) {
			final QueryableCollection collectionPersister = mapReference.getFromElement().getQueryableCollection();
			if ( Map.class.isAssignableFrom( collectionPersister.getCollectionType().getReturnedClass() ) ) {
				sourceFromElement = mapReference.getFromElement();
			}
		}
		else {
			if ( mapReference.getDataType().isCollectionType() ) {
				final CollectionType collectionType = (CollectionType) mapReference.getDataType();
				if ( Map.class.isAssignableFrom( collectionType.getReturnedClass() ) ) {
					sourceFromElement = mapReference.getFromElement();
				}
			}
		}

		if ( sourceFromElement == null ) {
			throw nonMap();
		}

		mapFromElement = sourceFromElement;

		setFromElement( sourceFromElement );
		setDataType( resolveType( sourceFromElement.getQueryableCollection() ) );
		this.columns = resolveColumns( sourceFromElement.getQueryableCollection() );
		initText( this.columns );
		setFirstChild( null );
	}

	public FromElement getMapFromElement() {
		return mapFromElement;
	}

	private boolean isAliasRef(FromReferenceNode mapReference) {
		return ALIAS_REF == mapReference.getType();
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

	@Override
	public void resolveIndex(AST parent) throws SemanticException {
		throw new UnsupportedOperationException( expressionDescription() + " expression cannot be the source for an index operation" );
	}

	protected MapKeyEntityFromElement findOrAddMapKeyEntityFromElement(QueryableCollection collectionPersister) {
		if ( !collectionPersister.getIndexType().isEntityType() ) {
			return null;
		}


		for ( FromElement destination : getFromElement().getDestinations() ) {
			if ( destination instanceof MapKeyEntityFromElement ) {
				return (MapKeyEntityFromElement) destination;
			}
		}

		return MapKeyEntityFromElement.buildKeyJoin( getFromElement() );
	}
}
