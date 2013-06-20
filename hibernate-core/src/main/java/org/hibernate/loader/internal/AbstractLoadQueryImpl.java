/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.loader.internal;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;

/**
 * Represents a generic load query used for generating SQL.
 *
 * This code is based on the SQL generation code originally in
 * org.hibernate.loader.JoinWalker.
 *
 * @author Gavin King
 * @author Jon Lipsky
 * @author Gail Badner
 */
public abstract class AbstractLoadQueryImpl {

	private final List<JoinableAssociation> associations;

	protected AbstractLoadQueryImpl(List<JoinableAssociation> associations) {
		this.associations = associations;
	}

	protected String orderBy(final String orderBy, AliasResolutionContext aliasResolutionContext) {
		return mergeOrderings( orderBy( associations, aliasResolutionContext ), orderBy );
	}

	protected static String mergeOrderings(String ordering1, String ordering2) {
		if ( ordering1.length() == 0 ) {
			return ordering2;
		}
		else if ( ordering2.length() == 0 ) {
			return ordering1;
		}
		else {
			return ordering1 + ", " + ordering2;
		}
	}

	/**
	 * Generate a sequence of <tt>LEFT OUTER JOIN</tt> clauses for the given associations.
	 */
	protected final JoinFragment mergeOuterJoins(SessionFactoryImplementor factory, AliasResolutionContext aliasResolutionContext)
	throws MappingException {
		JoinFragment joinFragment = factory.getDialect().createOuterJoinFragment();
		JoinableAssociation previous = null;
		for ( JoinableAssociation association : associations ) {
			final String rhsAlias = aliasResolutionContext.resolveAssociationRhsTableAlias( association );
			final String[] aliasedLhsColumnNames = aliasResolutionContext.resolveAssociationAliasedLhsColumnNames(
					association
			);
			final String[] rhsColumnNames = JoinHelper.getRHSColumnNames( association.getAssociationType(), factory );
			final String on = resolveOnCondition( factory, association, aliasResolutionContext );
			if ( previous != null && previous.isManyToManyWith( association ) ) {
				addManyToManyJoin(
						joinFragment,
						association,
						( QueryableCollection ) previous.getJoinable(),
						rhsAlias,
						aliasedLhsColumnNames,
						rhsColumnNames,
						on
				);
			}
			else {
				addJoins(
						joinFragment,
						association,
						rhsAlias,
						aliasedLhsColumnNames,
						rhsColumnNames,
						on
				);
			}
			previous = association;
		}
		return joinFragment;
	}

	/**
	 * Get the order by string required for collection fetching
	 */
	// TODO: why is this static?
	protected static String orderBy(
			List<JoinableAssociation> associations,
			AliasResolutionContext aliasResolutionContext)
	throws MappingException {
		StringBuilder buf = new StringBuilder();
		JoinableAssociation previous = null;
		for ( JoinableAssociation association : associations ) {
			final String rhsAlias = aliasResolutionContext.resolveAssociationRhsTableAlias( association );
			if ( association.getJoinType() == JoinType.LEFT_OUTER_JOIN ) { // why does this matter?
				if ( association.getJoinable().isCollection() ) {
					final QueryableCollection queryableCollection = (QueryableCollection) association.getJoinable();
					if ( queryableCollection.hasOrdering() ) {
						final String orderByString = queryableCollection.getSQLOrderByString( rhsAlias );
						buf.append( orderByString ).append(", ");
					}
				}
				else {
					// it might still need to apply a collection ordering based on a
					// many-to-many defined order-by...
					if ( previous != null && previous.getJoinable().isCollection() ) {
						final QueryableCollection queryableCollection = (QueryableCollection) previous.getJoinable();
						if ( queryableCollection.isManyToMany() && previous.isManyToManyWith( association ) ) {
							if ( queryableCollection.hasManyToManyOrdering() ) {
								final String orderByString = queryableCollection.getManyToManyOrderByString( rhsAlias );
								buf.append( orderByString ).append(", ");
							}
						}
					}
				}
			}
			previous = association;
		}
		if ( buf.length() > 0 ) {
			buf.setLength( buf.length() - 2 );
		}
		return buf.toString();
	}

	/**
	 * Render the where condition for a (batch) load by identifier / collection key
	 */
	protected StringBuilder whereString(String alias, String[] columnNames, int batchSize) {
		if ( columnNames.length==1 ) {
			// if not a composite key, use "foo in (?, ?, ?)" for batching
			// if no batch, and not a composite key, use "foo = ?"
			InFragment in = new InFragment().setColumn( alias, columnNames[0] );
			for ( int i = 0; i < batchSize; i++ ) {
				in.addValue( "?" );
			}
			return new StringBuilder( in.toFragmentString() );
		}
		else {
			//a composite key
			ConditionFragment byId = new ConditionFragment()
					.setTableAlias(alias)
					.setCondition( columnNames, "?" );
	
			StringBuilder whereString = new StringBuilder();
			if ( batchSize==1 ) {
				// if no batch, use "foo = ? and bar = ?"
				whereString.append( byId.toFragmentString() );
			}
			else {
				// if a composite key, use "( (foo = ? and bar = ?) or (foo = ? and bar = ?) )" for batching
				whereString.append('('); //TODO: unnecessary for databases with ANSI-style joins
				DisjunctionFragment df = new DisjunctionFragment();
				for ( int i=0; i<batchSize; i++ ) {
					df.addCondition(byId);
				}
				whereString.append( df.toFragmentString() );
				whereString.append(')'); //TODO: unnecessary for databases with ANSI-style joins
			}
			return whereString;
		}
	}

	/**
	 * Generate a select list of columns containing all properties of the entity classes
	 */
	protected final String associationSelectString(AliasResolutionContext aliasResolutionContext)
	throws MappingException {

		if ( associations.size() == 0 ) {
			return "";
		}
		else {
			StringBuilder buf = new StringBuilder( associations.size() * 100 );
			for ( int i=0; i<associations.size(); i++ ) {
				JoinableAssociation association = associations.get( i );
				JoinableAssociation next = ( i == associations.size() - 1 )
				        ? null
				        : associations.get( i + 1 );
				if ( !shouldAddToSql( association.getCurrentFetch() ) ) {
					continue;
				}

				final Joinable joinable = association.getJoinable();
				final EntityAliases currentEntityAliases =
						association.getCurrentEntityReference() == null ?
								null :
								aliasResolutionContext.resolveAliases( association.getCurrentEntityReference() ).getColumnAliases();
				final CollectionAliases currentCollectionAliases =
						association.getCurrentCollectionReference() == null ?
								null :
								aliasResolutionContext.resolveAliases( association.getCurrentCollectionReference() ).getCollectionColumnAliases();
				final String selectFragment = joinable.selectFragment(
						next == null ? null : next.getJoinable(),
						next == null ? null : aliasResolutionContext.resolveAssociationRhsTableAlias( next ),
						aliasResolutionContext.resolveAssociationRhsTableAlias( association ),
						currentEntityAliases == null ? null : currentEntityAliases.getSuffix(),
						currentCollectionAliases == null ? null : currentCollectionAliases.getSuffix(),
						association.getJoinType()==JoinType.LEFT_OUTER_JOIN
				);
				if (selectFragment.trim().length() > 0) {
					// TODO: shouldn't the append of selectFragment be outside this if statement???
					buf.append(", ").append( selectFragment );
				}
			}
			return buf.toString();
		}
	}

	private boolean shouldAddToSql(Fetch fetch) {
		return FetchStrategyHelper.isJoinFetched( fetch.getFetchStrategy() );
	}

	private void addJoins(
			JoinFragment joinFragment,
			JoinableAssociation association,
			String rhsAlias,
			String[] aliasedLhsColumnNames,
			String[] rhsColumnNames,
			String on) throws MappingException {
		joinFragment.addJoin(
				association.getJoinable().getTableName(),
				rhsAlias,
				aliasedLhsColumnNames,
				rhsColumnNames,
				association.getJoinType(),
				on
		);
		joinFragment.addJoins(
				association.getJoinable().fromJoinFragment( rhsAlias, false, true ),
				association.getJoinable().whereJoinFragment( rhsAlias, false, true )
		);
	}

	private String resolveOnCondition(
			SessionFactoryImplementor factory,
			JoinableAssociation joinableAssociation,
			AliasResolutionContext aliasResolutionContext) {
		final String withClause = StringHelper.isEmpty( joinableAssociation.getWithClause() ) ?
				"" :
				" and ( " + joinableAssociation.getWithClause() + " )";
		return joinableAssociation.getAssociationType().getOnCondition(
				aliasResolutionContext.resolveAssociationRhsTableAlias( joinableAssociation ),
				factory,
				joinableAssociation.getEnabledFilters()
		) + withClause;
	}

	/*
	public void validateJoin(String path) throws MappingException {
		if ( rhsColumns==null || lhsColumns==null
				|| lhsColumns.length!=rhsColumns.length || lhsColumns.length==0 ) {
			throw new MappingException("invalid join columns for association: " + path);
		}
	}
	*/

	private void addManyToManyJoin(
			JoinFragment outerjoin,
			JoinableAssociation association,
			QueryableCollection collection,
			String rhsAlias,
			String[] aliasedLhsColumnNames,
			String[] rhsColumnNames,
			String on) throws MappingException {
		final String manyToManyFilter = collection.getManyToManyFilterFragment(
				rhsAlias,
				association.getEnabledFilters()
		);
		String condition = "".equals( manyToManyFilter )
				? on
				: "".equals( on )
				? manyToManyFilter
				: on + " and " + manyToManyFilter;
		outerjoin.addJoin(
				association.getJoinable().getTableName(),
				rhsAlias,
				aliasedLhsColumnNames,
				rhsColumnNames,
				association.getJoinType(),
				condition
		);
		outerjoin.addJoins(
				association.getJoinable().fromJoinFragment( rhsAlias, false, true ),
				association.getJoinable().whereJoinFragment( rhsAlias, false, true )
		);
	}
}
