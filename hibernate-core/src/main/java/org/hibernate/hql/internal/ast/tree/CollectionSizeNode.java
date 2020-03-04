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
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.StandardBasicTypes;

import org.jboss.logging.Logger;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * @author Steve Ebersole
 */
public class CollectionSizeNode extends SqlNode implements SelectExpression {
	private static final Logger log = Logger.getLogger( CollectionSizeNode.class );

	private final CollectionPathNode collectionPathNode;
	private final CollectionPropertyMapping collectionPropertyMapping;

	private final HqlSqlWalker walker;
	private String alias;

	public CollectionSizeNode(CollectionPathNode collectionPathNode, HqlSqlWalker walker) {
		this.collectionPathNode = collectionPathNode;
		this.walker = walker;

		this.collectionPropertyMapping = new CollectionPropertyMapping( (QueryableCollection) collectionPathNode.getCollectionDescriptor() );

		setType( HqlSqlTokenTypes.COLL_SIZE );
		setDataType( StandardBasicTypes.INTEGER );
		setText( "collection-size" );
	}

	public CollectionPathNode getCollectionPathNode() {
		return collectionPathNode;
	}

	public HqlSqlWalker getWalker() {
		return walker;
	}

	public String toSqlExpression() throws SemanticException {
		// generate subquery in the form:
		//
		// select count( alias_.<collection-size-columns> )
		// from <collection-table> as alias_
		// where <owner-key-column> = alias_.<collection-key-column>

		// need:
		//		<collection-size-columns> 	=> QueryableCollection#getKeyColumnNames
		//		<collection-key-column>		=> QueryableCollection#getKeyColumnNames
		//		<collection-table> 			=> QueryableCollection#getTableName
		//		<owner-key-column>			=> ???


		final FromElement collectionOwnerFromElement = collectionPathNode.getCollectionOwnerRef();
		final QueryableCollection collectionDescriptor = (QueryableCollection) collectionPathNode.getCollectionDescriptor();
		final String collectionPropertyName = collectionPathNode.getCollectionPropertyName();

		getWalker().addQuerySpaces( collectionDescriptor.getCollectionSpaces() );

		// silly : need to prime `SessionFactoryHelper#collectionPropertyMappingByRole`
		walker.getSessionFactoryHelper().requireQueryableCollection( collectionDescriptor.getRole() );

		// owner-key
		final String[] ownerKeyColumns;
		final AST ast = walker.getAST();
		final String ownerTableAlias;
		if ( ast instanceof DeleteStatement || ast instanceof UpdateStatement ) {
			ownerTableAlias = collectionOwnerFromElement.getTableName();
		}
		else {
			ownerTableAlias = collectionOwnerFromElement.getTableAlias();
		}

		final String lhsPropertyName = collectionDescriptor.getCollectionType().getLHSPropertyName();
		if ( lhsPropertyName == null ) {
			ownerKeyColumns = StringHelper.qualify(
					ownerTableAlias,
					( (Joinable) collectionDescriptor.getOwnerEntityPersister() ).getKeyColumnNames()
			);
		}
		else {
			ownerKeyColumns = collectionOwnerFromElement.toColumns( ownerTableAlias, lhsPropertyName, true );
		}

		// collection-key
		final String collectionTableAlias = collectionOwnerFromElement.getFromClause()
				.getAliasGenerator()
				.createName( collectionPathNode.getCollectionPropertyName() );
		final String[] collectionKeyColumns = StringHelper.qualify( collectionTableAlias, collectionDescriptor.getKeyColumnNames() );


		if ( collectionKeyColumns.length != ownerKeyColumns.length ) {
			throw new AssertionFailure( "Mismatch between collection key columns" );
		}

		// PropertyMapping(c).toColumns(customers)
		// PropertyMapping(c.customers).toColumns(SIZE)

		// size expression (the count function)
		final String[] sizeColumns = this.collectionPropertyMapping.toColumns(
				collectionTableAlias,
				CollectionPropertyNames.COLLECTION_SIZE
		);
		assert sizeColumns.length == 1;
		final String sizeColumn = sizeColumns[0];

		final StringBuilder buffer = new StringBuilder( "(select " ).append( sizeColumn );
		buffer.append( " from " ).append( collectionDescriptor.getTableName() ).append( " as " ).append( collectionTableAlias );
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

	private String scalarName;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SelectExpression

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		log.debugf( "setScalarColumnText(%s)", i );
		scalarName = NameGenerator.scalarName( i, 0 );
	}

	@Override
	public void setScalarColumn(int i) throws SemanticException {
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
	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	@Override
	public boolean isScalar() throws SemanticException {
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
