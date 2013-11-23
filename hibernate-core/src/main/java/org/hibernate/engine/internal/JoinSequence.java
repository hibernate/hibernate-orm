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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.QueryJoinFragment;
import org.hibernate.type.AssociationType;

/**
 * A sequence of {@link Join} delegates to make it "easier" to work with joins.  The "easier" part is obviously
 * subjective ;)
 * <p/>
 * Additionally JoinSequence is a directed graph of other JoinSequence instances, as represented by the
 * {@link #next} ({@link #setNext(JoinSequence)}) pointer.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see JoinFragment
 */
public class JoinSequence {
	private final SessionFactoryImplementor factory;

	private final StringBuilder conditions = new StringBuilder();
	private final List<Join> joins = new ArrayList<Join>();

	private boolean useThetaStyle;
	private String rootAlias;
	private Joinable rootJoinable;
	private Selector selector;
	private JoinSequence next;
	private boolean isFromPart;

	/**
	 * Constructs a JoinSequence
	 *
	 * @param factory The SessionFactory
	 */
	public JoinSequence(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * Retrieve a JoinSequence that represents just the FROM clause parts
	 *
	 * @return The JoinSequence that represents just the FROM clause parts
	 */
	public JoinSequence getFromPart() {
		final JoinSequence fromPart = new JoinSequence( factory );
		fromPart.joins.addAll( this.joins );
		fromPart.useThetaStyle = this.useThetaStyle;
		fromPart.rootAlias = this.rootAlias;
		fromPart.rootJoinable = this.rootJoinable;
		fromPart.selector = this.selector;
		fromPart.next = this.next == null ? null : this.next.getFromPart();
		fromPart.isFromPart = true;
		return fromPart;
	}

	private Set<String> treatAsDeclarations;

	public void applyTreatAsDeclarations(Set<String> treatAsDeclarations) {
		if ( treatAsDeclarations == null || treatAsDeclarations.isEmpty() ) {
			return;
		}

		if ( this.treatAsDeclarations == null ) {
			this.treatAsDeclarations = new HashSet<String>();
		}

		this.treatAsDeclarations.addAll( treatAsDeclarations );
	}


	/**
	 * Create a full, although shallow, copy.
	 *
	 * @return The copy
	 */
	public JoinSequence copy() {
		final JoinSequence copy = new JoinSequence( factory );
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

	/**
	 * Add a join to this sequence
	 *
	 * @param associationType The type of the association representing the join
	 * @param alias The RHS alias for the join
	 * @param joinType The type of join (INNER, etc)
	 * @param referencingKey The LHS columns for the join condition
	 *
	 * @return The Join memento
	 *
	 * @throws MappingException Generally indicates a problem resolving the associationType to a {@link Joinable}
	 */
	public JoinSequence addJoin(
			AssociationType associationType,
			String alias,
			JoinType joinType,
			String[] referencingKey) throws MappingException {
		joins.add( new Join( factory, associationType, alias, joinType, referencingKey ) );
		return this;
	}

	/**
	 * Generate a JoinFragment
	 *
	 * @return The JoinFragment
	 *
	 * @throws MappingException Indicates a problem access the provided metadata, or incorrect metadata
	 */
	public JoinFragment toJoinFragment() throws MappingException {
		return toJoinFragment( Collections.EMPTY_MAP, true );
	}

	/**
	 * Generate a JoinFragment
	 *
	 * @param enabledFilters The filters associated with the originating session to properly define join conditions
	 * @param includeAllSubclassJoins Should all subclass joins be added to the rendered JoinFragment?
	 *
	 * @return The JoinFragment
	 *
	 * @throws MappingException Indicates a problem access the provided metadata, or incorrect metadata
	 */
	public JoinFragment toJoinFragment(Map enabledFilters, boolean includeAllSubclassJoins) throws MappingException {
		return toJoinFragment( enabledFilters, includeAllSubclassJoins, null, null );
	}

	/**
	 * Generate a JoinFragment
	 *
	 * @param enabledFilters The filters associated with the originating session to properly define join conditions
	 * @param includeAllSubclassJoins Should all subclass joins be added to the rendered JoinFragment?
	 * @param withClauseFragment The with clause (which represents additional join restrictions) fragment
	 * @param withClauseJoinAlias The
	 *
	 * @return The JoinFragment
	 *
	 * @throws MappingException Indicates a problem access the provided metadata, or incorrect metadata
	 */
	public JoinFragment toJoinFragment(
			Map enabledFilters,
			boolean includeAllSubclassJoins,
			String withClauseFragment,
			String withClauseJoinAlias) throws MappingException {
		final QueryJoinFragment joinFragment = new QueryJoinFragment( factory.getDialect(), useThetaStyle );
		if ( rootJoinable != null ) {
			joinFragment.addCrossJoin( rootJoinable.getTableName(), rootAlias );
			final String filterCondition = rootJoinable.filterFragment( rootAlias, enabledFilters, treatAsDeclarations );
			// JoinProcessor needs to know if the where clause fragment came from a dynamic filter or not so it
			// can put the where clause fragment in the right place in the SQL AST.   'hasFilterCondition' keeps track
			// of that fact.
			joinFragment.setHasFilterCondition( joinFragment.addCondition( filterCondition ) );
			addSubclassJoins( joinFragment, rootAlias, rootJoinable, true, includeAllSubclassJoins, treatAsDeclarations );
		}

		Joinable last = rootJoinable;

		for ( Join join : joins ) {
			// technically the treatAsDeclarations should only apply to rootJoinable or to a single Join,
			// but that is not possible atm given how these JoinSequence and Join objects are built.
			// However, it is generally ok given how the HQL parser builds these JoinSequences (a HQL join
			// results in a JoinSequence with an empty rootJoinable and a single Join).  So we use that here
			// as an assumption
			final String on = join.getAssociationType().getOnCondition( join.getAlias(), factory, enabledFilters, treatAsDeclarations );
			String condition;
			if ( last != null
					&& isManyToManyRoot( last )
					&& ((QueryableCollection) last).getElementType() == join.getAssociationType() ) {
				// the current join represents the join between a many-to-many association table
				// and its "target" table.  Here we need to apply any additional filters
				// defined specifically on the many-to-many
				final String manyToManyFilter = ( (QueryableCollection) last ).getManyToManyFilterFragment(
						join.getAlias(),
						enabledFilters
				);
				condition = "".equals( manyToManyFilter )
						? on
						: "".equals( on ) ? manyToManyFilter : on + " and " + manyToManyFilter;
			}
			else {
				condition = on;
			}

			if ( withClauseFragment != null && !isManyToManyRoot( join.joinable )) {
				condition += " and " + withClauseFragment;
			}

			joinFragment.addJoin(
					join.getJoinable().getTableName(),
					join.getAlias(),
					join.getLHSColumns(),
					JoinHelper.getRHSColumnNames( join.getAssociationType(), factory ),
					join.joinType,
					condition
			);

			addSubclassJoins(
					joinFragment,
					join.getAlias(),
					join.getJoinable(),
					join.joinType == JoinType.INNER_JOIN,
					includeAllSubclassJoins,
					// ugh.. this is needed because of how HQL parser (FromElementFactory/SessionFactoryHelper)
					// builds the JoinSequence for HQL joins
					treatAsDeclarations
			);
			last = join.getJoinable();
		}

		if ( next != null ) {
			joinFragment.addFragment( next.toJoinFragment( enabledFilters, includeAllSubclassJoins ) );
		}

		joinFragment.addCondition( conditions.toString() );

		if ( isFromPart ) {
			joinFragment.clearWherePart();
		}

		return joinFragment;
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isManyToManyRoot(Joinable joinable) {
		if ( joinable != null && joinable.isCollection() ) {
			return ( (QueryableCollection) joinable ).isManyToMany();
		}
		return false;
	}

	private void addSubclassJoins(
			JoinFragment joinFragment,
			String alias,
			Joinable joinable,
			boolean innerJoin,
			boolean includeSubclassJoins,
			Set<String> treatAsDeclarations) {
		final boolean include = includeSubclassJoins && isIncluded( alias );
		joinFragment.addJoins(
				joinable.fromJoinFragment( alias, innerJoin, include, treatAsDeclarations ),
				joinable.whereJoinFragment( alias, innerJoin, include, treatAsDeclarations )
		);
	}

	private boolean isIncluded(String alias) {
		return selector != null && selector.includeSubclasses( alias );
	}

	/**
	 * Add a condition to this sequence.
	 *
	 * @param condition The condition
	 *
	 * @return {@link this}, for method chaining
	 */
	public JoinSequence addCondition(String condition) {
		if ( condition.trim().length() != 0 ) {
			if ( !condition.startsWith( " and " ) ) {
				conditions.append( " and " );
			}
			conditions.append( condition );
		}
		return this;
	}

	/**
	 * Add a condition to this sequence.  Typical usage here might be:
	 * <pre>
	 *     addCondition( "a", {"c1", "c2"}, "?" )
	 * </pre>
	 * to represent:
	 * <pre>
	 *     "... a.c1 = ? and a.c2 = ? ..."
	 * </pre>
	 *
	 * @param alias The alias to apply to the columns
	 * @param columns The columns to add checks for
	 * @param condition The conditions to check against the columns
	 *
	 * @return {@link this}, for method chaining
	 */
	public JoinSequence addCondition(String alias, String[] columns, String condition) {
		for ( String column : columns ) {
			conditions.append( " and " )
					.append( alias )
					.append( '.' )
					.append( column )
					.append( condition );
		}
		return this;
	}

	/**
	 * Set the root of this JoinSequence.  In SQL terms, this would be the driving table.
	 *
	 * @param joinable The entity/collection that is the root of this JoinSequence
	 * @param alias The alias associated with that joinable.
	 *
	 * @return {@link this}, for method chaining
	 */
	public JoinSequence setRoot(Joinable joinable, String alias) {
		this.rootAlias = alias;
		this.rootJoinable = joinable;
		return this;
	}

	/**
	 * Sets the next join sequence
	 *
	 * @param next The next JoinSequence in the directed graph
	 *
	 * @return {@code this}, for method chaining
	 */
	public JoinSequence setNext(JoinSequence next) {
		this.next = next;
		return this;
	}

	/**
	 * Set the Selector to use to determine how subclass joins should be applied.
	 *
	 * @param selector The selector to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	public JoinSequence setSelector(Selector selector) {
		this.selector = selector;
		return this;
	}

	/**
	 * Should this JoinSequence use theta-style joining (both a FROM and WHERE component) in the rendered SQL?
	 *
	 * @param useThetaStyle {@code true} indicates that theta-style joins should be used.
	 *
	 * @return {@code this}, for method chaining
	 */
	public JoinSequence setUseThetaStyle(boolean useThetaStyle) {
		this.useThetaStyle = useThetaStyle;
		return this;
	}

	public boolean isThetaStyle() {
		return useThetaStyle;
	}

	public Join getFirstJoin() {
		return joins.get( 0 );
	}

	/**
	 * A subclass join selector
	 */
	public static interface Selector {
		/**
		 * Should subclasses be included in the rendered join sequence?
		 *
		 * @param alias The alias
		 *
		 * @return {@code true} if the subclass joins should be included
		 */
		public boolean includeSubclasses(String alias);
	}

	/**
	 * Represents a join
	 */
	public static final class Join {
		private final AssociationType associationType;
		private final Joinable joinable;
		private final JoinType joinType;
		private final String alias;
		private final String[] lhsColumns;

		Join(
				SessionFactoryImplementor factory,
				AssociationType associationType,
				String alias,
				JoinType joinType,
				String[] lhsColumns) throws MappingException {
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

	@Override
	public String toString() {
		final StringBuilder buf = new StringBuilder();
		buf.append( "JoinSequence{" );
		if ( rootJoinable != null ) {
			buf.append( rootJoinable )
					.append( '[' )
					.append( rootAlias )
					.append( ']' );
		}
		for ( Join join : joins ) {
			buf.append( "->" ).append( join );
		}
		return buf.append( '}' ).toString();
	}
}
