/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.OuterJoinableAssociation;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;

/**
 * Walker for one-to-many associations
 *
 * @author Gavin King
 * @see OneToManyLoader
 */
public class OneToManyJoinWalker extends CollectionJoinWalker {

	private final QueryableCollection oneToManyPersister;

	@Override
	protected boolean isDuplicateAssociation(
			final String foreignKeyTable,
			final String[] foreignKeyColumns) {
		//disable a join back to this same association
		final boolean isSameJoin = oneToManyPersister.getTableName().equals( foreignKeyTable ) &&
				Arrays.equals( foreignKeyColumns, oneToManyPersister.getKeyColumnNames() );
		return isSameJoin ||
				super.isDuplicateAssociation( foreignKeyTable, foreignKeyColumns );
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

		walkEntityTree( elementPersister, alias );

		List allAssociations = new ArrayList( associations );
		allAssociations.add(
				OuterJoinableAssociation.createRoot(
						oneToManyPersister.getCollectionType(),
						alias,
						getFactory()
				)
		);
		initPersisters( allAssociations, LockMode.NONE );
		initStatementString( elementPersister, alias, batchSize, subquery );
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

		StringBuilder whereString = whereString(
				alias,
				oneToManyPersister.getKeyColumnNames(),
				subquery,
				batchSize
		);
		String filter = oneToManyPersister.filterFragment( alias, getLoadQueryInfluencers().getEnabledFilters() );
		whereString.insert( 0, StringHelper.moveAndToBeginning( filter ) );

		JoinFragment ojf = mergeOuterJoins( associations );
		Select select = new Select( getDialect() )
				.setSelectClause(
						oneToManyPersister.selectFragment(
								null,
								null,
								alias,
								suffixes[joins],
								collectionSuffixes[0],
								true
						) +
								selectString( associations )
				)
				.setFromClause(
						elementPersister.fromTableFragment( alias ) +
								elementPersister.fromJoinFragment( alias, true, true )
				)
				.setWhereClause( whereString.toString() )
				.setOuterJoins(
						ojf.toFromFragmentString(),
						ojf.toWhereFragmentString() +
								elementPersister.whereJoinFragment( alias, true, true )
				);

		select.setOrderByClause( orderBy( associations, oneToManyPersister.getSQLOrderByString( alias ) ) );

		if ( getFactory().getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( "load one-to-many " + oneToManyPersister.getRole() );
		}

		sql = select.toStatementString();
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + oneToManyPersister.getRole() + ')';
	}

}
