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
package org.hibernate.loader.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.OuterJoinableAssociation;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.StringHelper;

/**
 * Walker for one-to-many associations
 *
 * @see OneToManyLoader
 * @author Gavin King
 */
public class OneToManyJoinWalker extends CollectionJoinWalker {

	private final QueryableCollection oneToManyPersister;

	protected boolean isDuplicateAssociation(
		final String foreignKeyTable, 
		final String[] foreignKeyColumns
	) {
		//disable a join back to this same association
		final boolean isSameJoin = oneToManyPersister.getTableName().equals(foreignKeyTable) &&
			Arrays.equals( foreignKeyColumns, oneToManyPersister.getKeyColumnNames() );
		return isSameJoin || 
			super.isDuplicateAssociation(foreignKeyTable, foreignKeyColumns);
	}

	public OneToManyJoinWalker(
			QueryableCollection oneToManyPersister, 
			int batchSize, 
			String subquery, 
			SessionFactoryImplementor factory, 
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( factory, loadQueryInfluencers );

		this.oneToManyPersister = oneToManyPersister;

		final OuterJoinLoadable elementPersister = (OuterJoinLoadable) oneToManyPersister.getElementPersister();
		final String alias = generateRootAlias( oneToManyPersister.getRole() );

		walkEntityTree(elementPersister, alias);

		List allAssociations = new ArrayList();
		allAssociations.addAll(associations);
		allAssociations.add( new OuterJoinableAssociation( 
				oneToManyPersister.getCollectionType(),
				null, 
				null, 
				alias, 
				JoinFragment.LEFT_OUTER_JOIN, 
				null,
				getFactory(), 
				CollectionHelper.EMPTY_MAP 
			) );
		
		initPersisters(allAssociations, LockMode.NONE);
		initStatementString(elementPersister, alias, batchSize, subquery);
	}

	private void initStatementString(
		final OuterJoinLoadable elementPersister,
		final String alias,
		final int batchSize,
		final String subquery)
	throws MappingException {

		final int joins = countEntityPersisters( associations );
		suffixes = BasicLoader.generateSuffixes( joins + 1 );

		final int collectionJoins = countCollectionPersisters( associations ) + 1;
		collectionSuffixes = BasicLoader.generateSuffixes( joins + 1, collectionJoins );

		StringBuffer whereString = whereString(
				alias, 
				oneToManyPersister.getKeyColumnNames(), 
				subquery,
				batchSize
			);
		String filter = oneToManyPersister.filterFragment( alias, getLoadQueryInfluencers().getEnabledFilters() );
		whereString.insert( 0, StringHelper.moveAndToBeginning(filter) );

		JoinFragment ojf = mergeOuterJoins(associations);
		Select select = new Select( getDialect() )
			.setSelectClause(
				oneToManyPersister.selectFragment(null, null, alias, suffixes[joins], collectionSuffixes[0], true) +
				selectString(associations)
			)
			.setFromClause(
				elementPersister.fromTableFragment(alias) +
				elementPersister.fromJoinFragment(alias, true, true)
			)
			.setWhereClause( whereString.toString() )
			.setOuterJoins(
				ojf.toFromFragmentString(),
				ojf.toWhereFragmentString() +
				elementPersister.whereJoinFragment(alias, true, true)
			);

		select.setOrderByClause( orderBy( associations, oneToManyPersister.getSQLOrderByString(alias) ) );

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load one-to-many " + oneToManyPersister.getRole() );
		}

		sql = select.toStatementString();
	}

	public String toString() {
		return getClass().getName() + '(' + oneToManyPersister.getRole() + ')';
	}

}
