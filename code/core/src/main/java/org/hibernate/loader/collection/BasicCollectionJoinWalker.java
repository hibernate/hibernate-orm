//$Id: BasicCollectionJoinWalker.java 9875 2006-05-04 16:23:44Z steve.ebersole@jboss.com $
package org.hibernate.loader.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.OuterJoinableAssociation;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.StringHelper;

/**
 * Walker for collections of values and many-to-many associations
 * 
 * @see BasicCollectionLoader
 * @author Gavin King
 */
public class BasicCollectionJoinWalker extends CollectionJoinWalker {
	
	private final QueryableCollection collectionPersister;

	public BasicCollectionJoinWalker(
			QueryableCollection collectionPersister, 
			int batchSize, 
			String subquery, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {

		super(factory, enabledFilters);

		this.collectionPersister = collectionPersister;

		String alias = generateRootAlias( collectionPersister.getRole() );

		walkCollectionTree(collectionPersister, alias);

		List allAssociations = new ArrayList();
		allAssociations.addAll(associations);
		allAssociations.add( new OuterJoinableAssociation( 
				collectionPersister.getCollectionType(),
				null, 
				null, 
				alias, 
				JoinFragment.LEFT_OUTER_JOIN, 
				getFactory(), 
				CollectionHelper.EMPTY_MAP 
			) );

		initPersisters(allAssociations, LockMode.NONE);
		initStatementString(alias, batchSize, subquery);

	}

	private void initStatementString(
		final String alias,
		final int batchSize,
		final String subquery)
	throws MappingException {

		final int joins = countEntityPersisters( associations );
		final int collectionJoins = countCollectionPersisters( associations ) + 1;

		suffixes = BasicLoader.generateSuffixes( joins );
		collectionSuffixes = BasicLoader.generateSuffixes( joins, collectionJoins );

		StringBuffer whereString = whereString(
				alias, 
				collectionPersister.getKeyColumnNames(), 
				subquery,
				batchSize
			);

		String manyToManyOrderBy = "";
		String filter = collectionPersister.filterFragment( alias, getEnabledFilters() );
		if ( collectionPersister.isManyToMany() ) {
			// from the collection of associations, locate OJA for the
			// ManyToOne corresponding to this persister to fully
			// define the many-to-many; we need that OJA so that we can
			// use its alias here
			// TODO : is there a better way here?
			Iterator itr = associations.iterator();
			AssociationType associationType = ( AssociationType ) collectionPersister.getElementType();
			while ( itr.hasNext() ) {
				OuterJoinableAssociation oja = ( OuterJoinableAssociation ) itr.next();
				if ( oja.getJoinableType() == associationType ) {
					// we found it
					filter += collectionPersister.getManyToManyFilterFragment( 
							oja.getRHSAlias(), 
							getEnabledFilters() 
						);
						manyToManyOrderBy += collectionPersister.getManyToManyOrderByString( oja.getRHSAlias() );
				}
			}
		}
		whereString.insert( 0, StringHelper.moveAndToBeginning( filter ) );

		JoinFragment ojf = mergeOuterJoins(associations);
		Select select = new Select( getDialect() )
			.setSelectClause(
				collectionPersister.selectFragment(alias, collectionSuffixes[0] ) +
				selectString(associations)
			)
			.setFromClause( collectionPersister.getTableName(), alias )
			.setWhereClause( whereString.toString()	)
			.setOuterJoins(
				ojf.toFromFragmentString(),
				ojf.toWhereFragmentString()
			);

		select.setOrderByClause( orderBy( associations, mergeOrderings( collectionPersister.getSQLOrderByString(alias), manyToManyOrderBy ) ) );

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load collection " + collectionPersister.getRole() );
		}

		sql = select.toStatementString();
	}

	/**
	 * We can use an inner join for first many-to-many association
	 */
	protected int getJoinType(
			AssociationType type, 
			FetchMode config, 
			String path, 
			Set visitedAssociations,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth)
	throws MappingException {

		int joinType = super.getJoinType(
				type, 
				config, 
				path, 
				lhsTable, 
				lhsColumns, 
				nullable, 
				currentDepth,
				null
			);
		//we can use an inner join for the many-to-many
		if ( joinType==JoinFragment.LEFT_OUTER_JOIN && "".equals(path) ) {
			joinType=JoinFragment.INNER_JOIN;
		}
		return joinType;
	}
	
	public String toString() {
		return getClass().getName() + '(' + collectionPersister.getRole() + ')';
	}


}
