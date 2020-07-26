/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.AssertionFailure;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.StandardBasicTypes;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionSizeNode extends SqlNode implements SelectExpression {
	private static final Logger log = Logger.getLogger( CollectionSizeNode.class );

	private final CollectionPathNode collectionPathNode;
	private final CollectionPropertyMapping collectionPropertyMapping;

	private String alias;

	public CollectionSizeNode(CollectionPathNode collectionPathNode) {
		this.collectionPathNode = collectionPathNode;
		this.collectionPropertyMapping = new CollectionPropertyMapping( (QueryableCollection) collectionPathNode.getCollectionDescriptor() );

		setType( HqlSqlTokenTypes.COLL_SIZE );
		setDataType( StandardBasicTypes.INTEGER );
		setText( "collection-size" );
	}

	@SuppressWarnings("unused")
	public CollectionPathNode getCollectionPathNode() {
		return collectionPathNode;
	}

	public String toSqlExpression() {
		final FromElement collectionOwnerFromElement = collectionPathNode.getCollectionOwnerFromElement();
		final QueryableCollection collectionDescriptor = (QueryableCollection) collectionPathNode.getCollectionDescriptor();

		// generate subquery in the form:
		//
		// select count( alias_.<collection-key-column> )
		// from <collection-table> as alias_
		// where <owner-key-column> = alias_.<collection-key-column>

		// Note that `collectionPropertyMapping.toColumns(.., COLLECTION_SIZE)` returns the complete `count(...)` SQL
		// expression, hence he expectation for a single expression regardless of the number of columns in the key.

		final String collectionTableAlias = collectionOwnerFromElement.getFromClause()
				.getAliasGenerator()
				.createName( collectionPathNode.getCollectionPropertyName() );

		final String[] ownerKeyColumns = collectionPathNode.resolveOwnerKeyColumnExpressions();
		final String[] collectionKeyColumns = StringHelper.qualify( collectionTableAlias, collectionDescriptor.getKeyColumnNames() );

		if ( collectionKeyColumns.length != ownerKeyColumns.length ) {
			throw new AssertionFailure( "Mismatch between collection key columns" );
		}

		final String[] sizeColumns = this.collectionPropertyMapping.toColumns(
				collectionTableAlias,
				CollectionPropertyNames.COLLECTION_SIZE
		);
		assert sizeColumns.length == 1;
		final String sizeColumn = sizeColumns[0];

		final StringBuilder buffer = new StringBuilder( "(select " ).append( sizeColumn );
		buffer.append( " from " ).append( collectionDescriptor.getTableName() ).append( " " ).append( collectionTableAlias );
		buffer.append( " where " );

		boolean firstPass = true;
		for ( int i = 0; i < ownerKeyColumns.length; i++ ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				buffer.append( " and " );
			}

			buffer.append( ownerKeyColumns[i] ).append( " = " ).append( collectionKeyColumns[i] );
		}

		buffer.append( ")" );

		if ( scalarName != null ) {
			buffer.append( " as " ).append( scalarName );
		}

		final String subQuery = buffer.toString();

		log.debugf(
				"toSqlExpression( size(%s) ) -> %s",
				collectionPathNode.getCollectionQueryPath(),
				subQuery
		);

		return subQuery;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SelectExpression

	private String scalarName;

	@Override
	public void setScalarColumnText(int i) {
		log.debugf( "setScalarColumnText(%s)", i );
		scalarName = NameGenerator.scalarName( i, 0 );
	}

	@Override
	public void setScalarColumn(int i) {
		log.debugf( "setScalarColumn(%s)", i );
		setScalarColumnText( i );
	}

	@Override
	public int getScalarColumnIndex() {
		return -1;
	}

	@Override
	public FromElement getFromElement() {
		return null;
	}

	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public boolean isReturnableEntity() {
		return false;
	}

	@Override
	public boolean isScalar() {
		return false;
	}

	@Override
	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String getAlias() {
		return alias;
	}
}
