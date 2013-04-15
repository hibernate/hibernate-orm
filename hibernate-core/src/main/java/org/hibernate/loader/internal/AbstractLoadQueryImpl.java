/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;

/**
 * Walks the metamodel, searching for joins, and collecting
 * together information needed by <tt>OuterJoinLoader</tt>.
 * 
 * @see org.hibernate.loader.OuterJoinLoader
 * @author Gavin King, Jon Lipsky
 */
public abstract class AbstractLoadQueryImpl {

	private final SessionFactoryImplementor factory;
	private final List<JoinableAssociationImpl> associations;

	protected AbstractLoadQueryImpl(
			SessionFactoryImplementor factory,
			List<JoinableAssociationImpl> associations) {
		this.factory = factory;
		this.associations = associations;
	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected Dialect getDialect() {
		return factory.getDialect();
	}

	protected String orderBy(final String orderBy) {
		return mergeOrderings( orderBy( associations ), orderBy );
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
	protected final JoinFragment mergeOuterJoins()
	throws MappingException {
		JoinFragment outerjoin = getDialect().createOuterJoinFragment();
		JoinableAssociationImpl last = null;
		for ( JoinableAssociationImpl oj : associations ) {
			if ( last != null && last.isManyToManyWith( oj ) ) {
				oj.addManyToManyJoin( outerjoin, ( QueryableCollection ) last.getJoinable() );
			}
			else {
				oj.addJoins(outerjoin);
			}
			last = oj;
		}
		return outerjoin;
	}

	/**
	 * Get the order by string required for collection fetching
	 */
	protected static String orderBy(List<JoinableAssociationImpl> associations)
	throws MappingException {
		StringBuilder buf = new StringBuilder();
		JoinableAssociationImpl last = null;
		for ( JoinableAssociationImpl oj : associations ) {
			if ( oj.getJoinType() == JoinType.LEFT_OUTER_JOIN ) { // why does this matter?
				if ( oj.getJoinable().isCollection() ) {
					final QueryableCollection queryableCollection = (QueryableCollection) oj.getJoinable();
					if ( queryableCollection.hasOrdering() ) {
						final String orderByString = queryableCollection.getSQLOrderByString( oj.getRHSAlias() );
						buf.append( orderByString ).append(", ");
					}
				}
				else {
					// it might still need to apply a collection ordering based on a
					// many-to-many defined order-by...
					if ( last != null && last.getJoinable().isCollection() ) {
						final QueryableCollection queryableCollection = (QueryableCollection) last.getJoinable();
						if ( queryableCollection.isManyToMany() && last.isManyToManyWith( oj ) ) {
							if ( queryableCollection.hasManyToManyOrdering() ) {
								final String orderByString = queryableCollection.getManyToManyOrderByString( oj.getRHSAlias() );
								buf.append( orderByString ).append(", ");
							}
						}
					}
				}
			}
			last = oj;
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
	protected final String associationSelectString()
	throws MappingException {

		if ( associations.size() == 0 ) {
			return "";
		}
		else {
			StringBuilder buf = new StringBuilder( associations.size() * 100 );
			for ( int i=0; i<associations.size(); i++ ) {
				JoinableAssociationImpl join = associations.get(i);
				JoinableAssociationImpl next = (i == associations.size() - 1)
				        ? null
				        : associations.get( i + 1 );
				final Joinable joinable = join.getJoinable();
				final String selectFragment = joinable.selectFragment(
						next == null ? null : next.getJoinable(),
						next == null ? null : next.getRHSAlias(),
						join.getRHSAlias(),
						associations.get( i ).getCurrentEntitySuffix(),
						associations.get( i ).getCurrentCollectionSuffix(),
						join.getJoinType()==JoinType.LEFT_OUTER_JOIN
				);
				if (selectFragment.trim().length() > 0) {
					buf.append(", ").append(selectFragment);
				}
			}
			return buf.toString();
		}
	}
}
