/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.QueryJoinFragment;
import org.hibernate.type.AssociationType;

/**
 * @author Gavin King
 */
public class JoinSequence {

	private final SessionFactoryImplementor factory;
	private final List<Join> joins = new ArrayList<Join>();
	private boolean useThetaStyle = false;
	private final StringBuilder conditions = new StringBuilder();
	private String rootAlias;
	private Joinable rootJoinable;
	private Selector selector;
	private JoinSequence next;
	private boolean isFromPart = false;

	@Override
    public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append( "JoinSequence{" );
		if ( rootJoinable != null ) {
			buf.append( rootJoinable )
					.append( '[' )
					.append( rootAlias )
					.append( ']' );
		}
		for ( int i = 0; i < joins.size(); i++ ) {
			buf.append( "->" ).append( joins.get( i ) );
		}
		return buf.append( '}' ).toString();
	}

	public final class Join {

		private final AssociationType associationType;
		private final Joinable joinable;
		private final JoinType joinType;
		private final String alias;
		private final String[] lhsColumns;

		Join(AssociationType associationType, String alias, JoinType joinType, String[] lhsColumns)
				throws MappingException {
			this.associationType = associationType;
			this.joinable = associationType.getAssociatedJoinable( factory );
			this.alias = alias;
			this.joinType = joinType;
			this.lhsColumns = lhsColumns;
		}

		public String getAlias() {
			return alias;
		}

		public AssociationType getAssociationType() {
			return associationType;
		}

		public Joinable getJoinable() {
			return joinable;
		}

		public JoinType getJoinType() {
			return joinType;
		}

		public String[] getLHSColumns() {
			return lhsColumns;
		}

		@Override
        public String toString() {
			return joinable.toString() + '[' + alias + ']';
		}
	}

	public JoinSequence(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	public JoinSequence getFromPart() {
		JoinSequence fromPart = new JoinSequence( factory );
		fromPart.joins.addAll( this.joins );
		fromPart.useThetaStyle = this.useThetaStyle;
		fromPart.rootAlias = this.rootAlias;
		fromPart.rootJoinable = this.rootJoinable;
		fromPart.selector = this.selector;
		fromPart.next = this.next == null ? null : this.next.getFromPart();
		fromPart.isFromPart = true;
		return fromPart;
	}

	public JoinSequence copy() {
		JoinSequence copy = new JoinSequence( factory );
		copy.joins.addAll( this.joins );
		copy.useThetaStyle = this.useThetaStyle;
		copy.rootAlias = this.rootAlias;
		copy.rootJoinable = this.rootJoinable;
		copy.selector = this.selector;
		copy.next = this.next == null ? null : this.next.copy();
		copy.isFromPart = this.isFromPart;
		copy.conditions.append( this.conditions.toString() );
		return copy;
	}

	public JoinSequence addJoin(AssociationType associationType, String alias, JoinType joinType, String[] referencingKey)
			throws MappingException {
		joins.add( new Join( associationType, alias, joinType, referencingKey ) );
		return this;
	}

	public JoinFragment toJoinFragment() throws MappingException {
		return toJoinFragment( Collections.EMPTY_MAP, true );
	}

	public JoinFragment toJoinFragment(Map enabledFilters, boolean includeExtraJoins) throws MappingException {
		return toJoinFragment( enabledFilters, includeExtraJoins, null, null );
	}

	public JoinFragment toJoinFragment(
			Map enabledFilters,
	        boolean includeExtraJoins,
	        String withClauseFragment,
	        String withClauseJoinAlias) throws MappingException {
		QueryJoinFragment joinFragment = new QueryJoinFragment( factory.getDialect(), useThetaStyle );
		if ( rootJoinable != null ) {
			joinFragment.addCrossJoin( rootJoinable.getTableName(), rootAlias );
			String filterCondition = rootJoinable.filterFragment( rootAlias, enabledFilters );
			// JoinProcessor needs to know if the where clause fragment came from a dynamic filter or not so it
			// can put the where clause fragment in the right place in the SQL AST.   'hasFilterCondition' keeps track
			// of that fact.
			joinFragment.setHasFilterCondition( joinFragment.addCondition( filterCondition ) );
			if (includeExtraJoins) { //TODO: not quite sure about the full implications of this!
				addExtraJoins( joinFragment, rootAlias, rootJoinable, true );
			}
		}

		Joinable last = rootJoinable;

		for ( Join join: joins ) {
			String on = join.getAssociationType().getOnCondition( join.getAlias(), factory, enabledFilters );
			String condition = null;
			if ( last != null &&
			        isManyToManyRoot( last ) &&
			        ( ( QueryableCollection ) last ).getElementType() == join.getAssociationType() ) {
				// the current join represents the join between a many-to-many association table
				// and its "target" table.  Here we need to apply any additional filters
				// defined specifically on the many-to-many
				String manyToManyFilter = ( ( QueryableCollection ) last )
				        .getManyToManyFilterFragment( join.getAlias(), enabledFilters );
				condition = "".equals( manyToManyFilter )
						? on
						: "".equals( on )
								? manyToManyFilter
								: on + " and " + manyToManyFilter;
			}
			else {
				condition = on;
			}
			if ( withClauseFragment != null ) {
				if ( join.getAlias().equals( withClauseJoinAlias ) ) {
					condition += " and " + withClauseFragment;
				}
			}
			joinFragment.addJoin(
			        join.getJoinable().getTableName(),
					join.getAlias(),
					join.getLHSColumns(),
					JoinHelper.getRHSColumnNames( join.getAssociationType(), factory ),
					join.joinType,
					condition
			);
			if (includeExtraJoins) { //TODO: not quite sure about the full implications of this!
				addExtraJoins( joinFragment, join.getAlias(), join.getJoinable(), join.joinType == JoinType.INNER_JOIN );
			}
			last = join.getJoinable();
		}
		if ( next != null ) {
			joinFragment.addFragment( next.toJoinFragment( enabledFilters, includeExtraJoins ) );
		}
		joinFragment.addCondition( conditions.toString() );
		if ( isFromPart ) joinFragment.clearWherePart();
		return joinFragment;
	}

	private boolean isManyToManyRoot(Joinable joinable) {
		if ( joinable != null && joinable.isCollection() ) {
			QueryableCollection persister = ( QueryableCollection ) joinable;
			return persister.isManyToMany();
		}
		return false;
	}

	private boolean isIncluded(String alias) {
		return selector != null && selector.includeSubclasses( alias );
	}

	private void addExtraJoins(JoinFragment joinFragment, String alias, Joinable joinable, boolean innerJoin) {
		boolean include = isIncluded( alias );
		joinFragment.addJoins( joinable.fromJoinFragment( alias, innerJoin, include ),
				joinable.whereJoinFragment( alias, innerJoin, include ) );
	}

	public JoinSequence addCondition(String condition) {
		if ( condition.trim().length() != 0 ) {
			if ( !condition.startsWith( " and " ) ) conditions.append( " and " );
			conditions.append( condition );
		}
		return this;
	}

	public JoinSequence addCondition(String alias, String[] columns, String condition) {
		for ( int i = 0; i < columns.length; i++ ) {
			conditions.append( " and " )
					.append( alias )
					.append( '.' )
					.append( columns[i] )
					.append( condition );
		}
		return this;
	}

	public JoinSequence setRoot(Joinable joinable, String alias) {
		this.rootAlias = alias;
		this.rootJoinable = joinable;
		return this;
	}

	public JoinSequence setNext(JoinSequence next) {
		this.next = next;
		return this;
	}

	public JoinSequence setSelector(Selector s) {
		this.selector = s;
		return this;
	}

	public JoinSequence setUseThetaStyle(boolean useThetaStyle) {
		this.useThetaStyle = useThetaStyle;
		return this;
	}

	public boolean isThetaStyle() {
		return useThetaStyle;
	}

	public int getJoinCount() {
		return joins.size();
	}

	public Iterator iterateJoins() {
		return joins.iterator();
	}

	public Join getFirstJoin() {
		return (Join) joins.get( 0 );
	}

	public static interface Selector {
		public boolean includeSubclasses(String alias);
	}
}
