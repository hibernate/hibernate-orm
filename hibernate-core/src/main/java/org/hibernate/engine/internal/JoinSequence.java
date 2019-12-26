/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.*;
import java.util.Collections;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.tree.ImpliedFromElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
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
	private final boolean collectionJoinSubquery;

	private final StringBuilder conditions = new StringBuilder();
	private final List<Join> joins = new ArrayList<Join>();

	private boolean useThetaStyle;
	private String rootAlias;
	private Joinable rootJoinable;
	private Selector selector;
	private JoinSequence next;
	private boolean isFromPart;
	private Set<String> queryReferencedTables;

	/**
	 * Constructs a JoinSequence
	 *
	 * @param factory The SessionFactory
	 */
	public JoinSequence(SessionFactoryImplementor factory) {
		this.factory = factory;
		this.collectionJoinSubquery = factory.getSessionFactoryOptions().isCollectionJoinSubqueryRewriteEnabled();
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

	protected Set<String> getTreatAsDeclarations() {
		return treatAsDeclarations;
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
		joins.add( new Join( factory, associationType, alias, joinType, new String[][] { referencingKey } ) );
		return this;
	}

	/**
	 * Add a join to this sequence
	 *
	 * @param associationType The type of the association representing the join
	 * @param alias The RHS alias for the join
	 * @param joinType The type of join (INNER, etc)
	 * @param referencingKeys The LHS columns for the join condition
	 *
	 * @return The Join memento
	 *
	 * @throws MappingException Generally indicates a problem resolving the associationType to a {@link Joinable}
	 */
	public JoinSequence addJoin(
			AssociationType associationType,
			String alias,
			JoinType joinType,
			String[][] referencingKeys) throws MappingException {
		joins.add( new Join( factory, associationType, alias, joinType, referencingKeys ) );
		return this;
	}

	/**
	 * Embeds an implied from element into this sequence
	 *
	 * @param fromElement The implied from element to embed
	 * @return The Join memento
	 */
	public JoinSequence addJoin(ImpliedFromElement fromElement) {
		joins.addAll( fromElement.getJoinSequence().joins );
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
		return toJoinFragment( enabledFilters, includeAllSubclassJoins, null );
	}

	/**
	 * Generate a JoinFragment
	 *
	 * @param enabledFilters The filters associated with the originating session to properly define join conditions
	 * @param includeAllSubclassJoins Should all subclass joins be added to the rendered JoinFragment?
	 * @param withClauseFragment The with clause (which represents additional join restrictions) fragment
	 *
	 * @return The JoinFragment
	 *
	 * @throws MappingException Indicates a problem access the provided metadata, or incorrect metadata
	 */
	public JoinFragment toJoinFragment(
			Map enabledFilters,
			boolean includeAllSubclassJoins,
			String withClauseFragment) throws MappingException {
		return toJoinFragment( enabledFilters, includeAllSubclassJoins, true, withClauseFragment );
	}

	public JoinFragment toJoinFragment(
			Map enabledFilters,
			boolean includeAllSubclassJoins,
			boolean renderSubclassJoins,
			String withClauseFragment) throws MappingException {
		final QueryJoinFragment joinFragment = new QueryJoinFragment( factory.getDialect(), useThetaStyle );
		Iterator<Join> iter;
		Join first;
		Joinable last;
		if ( rootJoinable != null ) {
			joinFragment.addCrossJoin( rootJoinable.getTableName(), rootAlias );
			final String filterCondition = rootJoinable.filterFragment( rootAlias, enabledFilters, treatAsDeclarations );
			// JoinProcessor needs to know if the where clause fragment came from a dynamic filter or not so it
			// can put the where clause fragment in the right place in the SQL AST.   'hasFilterCondition' keeps track
			// of that fact.
			joinFragment.setHasFilterCondition( joinFragment.addCondition( filterCondition ) );
			addSubclassJoins( joinFragment, rootAlias, rootJoinable, true, includeAllSubclassJoins, treatAsDeclarations );

			last = rootJoinable;
		}
		else if ( needsTableGroupJoin( joins, withClauseFragment ) ) {
			iter = joins.iterator();
			first = iter.next();
			final String joinString;
			switch (first.joinType) {
				case INNER_JOIN:
					joinString = " inner join ";
					break;
				case LEFT_OUTER_JOIN:
					joinString = " left outer join ";
					break;
				case RIGHT_OUTER_JOIN:
					joinString = " right outer join ";
					break;
				case FULL_JOIN:
					joinString = " full outer join ";
					break;
				default:
					throw new AssertionFailure("undefined join type");
			}

			joinFragment.addFromFragmentString( joinString );
			joinFragment.addFromFragmentString( " (" );
			joinFragment.addFromFragmentString( first.joinable.getTableName() );
			joinFragment.addFromFragmentString( " " );
			joinFragment.addFromFragmentString( first.getAlias() );

			for ( Join join : joins ) {
				// Skip joining the first join node as it is contained in the subquery
				if ( join != first ) {
					joinFragment.addJoin(
							join.getJoinable().getTableName(),
							join.getAlias(),
							join.getLHSColumns(),
							JoinHelper.getRHSColumnNames( join.getAssociationType(), factory ),
							join.joinType
					);
				}
				addSubclassJoins(
						joinFragment,
						join.getAlias(),
						join.getJoinable(),
						// TODO: Think about if this could be made always true
						join.joinType == JoinType.INNER_JOIN,
						includeAllSubclassJoins,
						// ugh.. this is needed because of how HQL parser (FromElementFactory/SessionFactoryHelper)
						// builds the JoinSequence for HQL joins
						treatAsDeclarations
				);
			}

			joinFragment.addFromFragmentString( ")" );
			joinFragment.addFromFragmentString( " on " );

			final String rhsAlias = first.getAlias();
			final String[][] lhsColumns = first.getLHSColumns();
			final String[] rhsColumns = JoinHelper.getRHSColumnNames( first.getAssociationType(), factory );
			if ( lhsColumns.length > 1 ) {
				joinFragment.addFromFragmentString( "(" );
			}
			for ( int i = 0; i < lhsColumns.length; i++ ) {
				for ( int j = 0; j < lhsColumns[i].length; j++ ) {
					joinFragment.addFromFragmentString( lhsColumns[i][j] );
					joinFragment.addFromFragmentString( "=" );
					joinFragment.addFromFragmentString( rhsAlias );
					joinFragment.addFromFragmentString( "." );
					joinFragment.addFromFragmentString( rhsColumns[j] );
					if ( j < lhsColumns[i].length - 1 ) {
						joinFragment.addFromFragmentString( " and " );
					}
				}
				if ( i < lhsColumns.length - 1 ) {
					joinFragment.addFromFragmentString( " or " );
				}
			}
			if ( lhsColumns.length > 1 ) {
				joinFragment.addFromFragmentString( ")" );
			}

			joinFragment.addFromFragmentString( " and " );
			joinFragment.addFromFragmentString( withClauseFragment );

			return joinFragment;
		}
		else {
			last = null;
		}
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

			if (renderSubclassJoins) {
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
			}

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

	private boolean needsTableGroupJoin(List<Join> joins, String withClauseFragment) {
		// If the rewrite is disabled or we don't have a with clause, we don't need a table group join
		if ( !collectionJoinSubquery || StringHelper.isEmpty( withClauseFragment ) ) {
			return false;
		}
		// If we only have one join, a table group join is only necessary if subclass columns are used in the with clause
		if ( joins.size() < 2 ) {
			return isSubclassAliasDereferenced( joins.get( 0 ), withClauseFragment );
		}
		// If more than one table is involved and this is not an inner join, we definitely need a table group join
		// i.e. a left join has to be made for the table group to retain the join semantics
		if ( joins.get( 0 ).getJoinType() != JoinType.INNER_JOIN ) {
			return true;
		}
		// If a subclass columns is used, we need a table group, otherwise we generate wrong SQL by putting the ON condition to the first join
		if ( isSubclassAliasDereferenced( joins.get( 0 ), withClauseFragment ) ) {
			return true;
		}

		// Normally, the ON condition of a HQL join is put on the ON clause of the first SQL join
		// Since the ON condition could refer to columns from subsequently joined tables i.e. joins with index > 0
		// or could refer to columns of subclass tables, the SQL could be wrong
		// To avoid generating wrong SQL, we detect these cases here i.e. a subsequent join alias is used in the ON condition
		// If we find out that this is the case, we return true and generate a table group join

		// Skip the first since that is the driving join
		for ( int i = 1; i < joins.size(); i++ ) {
			Join join = joins.get( i );

			if ( isAliasDereferenced( withClauseFragment, join.getAlias() ) || isSubclassAliasDereferenced( join, withClauseFragment ) ) {
				return true;
			}
		}

		return false;
	}

	private boolean isSubclassAliasDereferenced(Join join, String withClauseFragment) {
		if ( join.getJoinable() instanceof AbstractEntityPersister ) {
			AbstractEntityPersister persister = (AbstractEntityPersister) join.getJoinable();
			int subclassTableSpan = persister.getSubclassTableSpan();
			for ( int j = 1; j < subclassTableSpan; j++ ) {
				String subclassAlias = AbstractEntityPersister.generateTableAlias( join.getAlias(), j );
				if ( isAliasDereferenced( withClauseFragment, subclassAlias ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isAliasDereferenced(String withClauseFragment, String alias) {
		// See if the with clause contains the join alias
		int index = withClauseFragment.indexOf( alias );
		int dotIndex = index + alias.length();
		if ( index != -1
				// Check that the join alias is not a suffix
				&& ( index == 0 || !Character.isLetterOrDigit( withClauseFragment.charAt( index - 1 ) ) )
				// Check that the join alias gets de-referenced i.e. the next char is a dot
				&& dotIndex < withClauseFragment.length() && withClauseFragment.charAt( dotIndex ) == '.' ) {
			return true;
		}

		return false;
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
				joinable.fromJoinFragment( alias, innerJoin, include, treatAsDeclarations, queryReferencedTables ),
				joinable.whereJoinFragment( alias, innerJoin, include, treatAsDeclarations )
		);
	}

	protected boolean isIncluded(String alias) {
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

	/**
	 * Set all tables the query refers to. It allows to optimize the query.
	 *
	 * @param queryReferencedTables
	 */
	public void setQueryReferencedTables(Set<String> queryReferencedTables) {
		this.queryReferencedTables = queryReferencedTables;
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
		private final String[][] lhsColumns;

		Join(
				SessionFactoryImplementor factory,
				AssociationType associationType,
				String alias,
				JoinType joinType,
				String[][] lhsColumns) throws MappingException {
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

		public String[][] getLHSColumns() {
			return lhsColumns;
		}

		@Override
		public String toString() {
			return joinable.toString() + '[' + alias + ']';
		}
	}

	public JoinSequence copyForCollectionProperty() {
		JoinSequence copy = this.copy();
		copy.joins.clear();
		Iterator<Join> joinIterator = this.joins.iterator();
		while ( joinIterator.hasNext() ) {
			Join join = joinIterator.next();
			copy.addJoin(
					join.getAssociationType(),
					join.getAlias(),
					JoinType.INNER_JOIN,
					join.getLHSColumns()
			);
		}
		return copy;
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
