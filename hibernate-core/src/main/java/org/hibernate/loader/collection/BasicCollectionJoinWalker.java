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
import java.util.Iterator;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.OuterJoinableAssociation;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;

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
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {

		super( factory, loadQueryInfluencers );

		this.collectionPersister = collectionPersister;

		String alias = generateRootAlias( collectionPersister.getRole() );

		walkCollectionTree(collectionPersister, alias);

		List allAssociations = new ArrayList();
		allAssociations.addAll(associations);
		allAssociations.add( OuterJoinableAssociation.createRoot( collectionPersister.getCollectionType(), alias, getFactory() ) );
		initPersisters(allAssociations, LockMode.NONE);
		initStatementString(alias, batchSize, subquery);
	}

	private void initStatementString(
		final String alias,
		final int batchSize,
		final String subquery) throws MappingException {

		final int joins = countEntityPersisters( associations );
		final int collectionJoins = countCollectionPersisters( associations ) + 1;

		suffixes = BasicLoader.generateSuffixes( joins );
		collectionSuffixes = BasicLoader.generateSuffixes( joins, collectionJoins );

		StringBuilder whereString = whereString(
				alias, 
				collectionPersister.getKeyColumnNames(), 
				subquery,
				batchSize
			);

		String manyToManyOrderBy = "";
		String filter = collectionPersister.filterFragment( alias, getLoadQueryInfluencers().getEnabledFilters() );
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
							getLoadQueryInfluencers().getEnabledFilters() 
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

	protected JoinType getJoinType(
			OuterJoinLoadable persister,
			PropertyPath path,
			int propertyNumber,
			AssociationType associationType,
			FetchMode metadataFetchMode,
			CascadeStyle metadataCascadeStyle,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth) throws MappingException {
		JoinType joinType = super.getJoinType(
				persister,
				path,
				propertyNumber,
				associationType,
				metadataFetchMode,
				metadataCascadeStyle,
				lhsTable,
				lhsColumns,
				nullable,
				currentDepth
		);
		//we can use an inner join for the many-to-many
		if ( joinType==JoinType.LEFT_OUTER_JOIN && path.isRoot() ) {
			joinType=JoinType.INNER_JOIN;
		}
		return joinType;
	}

	public String toString() {
		return getClass().getName() + '(' + collectionPersister.getRole() + ')';
	}


}
