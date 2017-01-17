/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.*;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private final boolean collectionJoinSubquery;

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
		return toJoinFragment( enabledFilters, includeAllSubclassJoins, null, null, null );
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
			String withClauseJoinAlias,
			String withClauseCollectionJoinAlias) throws MappingException {
		return toJoinFragment( enabledFilters, includeAllSubclassJoins, true, withClauseFragment, withClauseJoinAlias, withClauseCollectionJoinAlias );
	}

	public JoinFragment toJoinFragment(
			Map enabledFilters,
			boolean includeAllSubclassJoins,
			boolean renderSubclassJoins,
			String withClauseFragment,
			String withClauseJoinAlias,
			String withClauseCollectionJoinAlias) throws MappingException {
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
		else if (
				collectionJoinSubquery
				&& withClauseFragment != null
				&& joins.size() > 1
				&& ( withClauseFragment.contains( withClauseJoinAlias ) || ( withClauseCollectionJoinAlias != null && withClauseFragment.contains( withClauseCollectionJoinAlias ) ) )
				&& ( first = ( iter = joins.iterator() ).next() ).joinType == JoinType.LEFT_OUTER_JOIN
				) {
			final QueryJoinFragment subqueryJoinFragment = new QueryJoinFragment( factory.getDialect(), useThetaStyle );
			subqueryJoinFragment.addFromFragmentString( "(select " );

			subqueryJoinFragment.addFromFragmentString( first.getAlias() );
			subqueryJoinFragment.addFromFragmentString( ".*" );

			// Re-alias columns of withClauseJoinAlias and rewrite withClauseFragment
			// A list of possible delimited identifier types: https://en.wikibooks.org/wiki/SQL_Dialects_Reference/Data_structure_definition/Delimited_identifiers
			String prefixPattern = "(" + Pattern.quote( withClauseJoinAlias );
			if ( withClauseCollectionJoinAlias != null ) {
				prefixPattern += "|" + Pattern.quote( withClauseCollectionJoinAlias );
			}
			prefixPattern += ")" + Pattern.quote( "." );
			Pattern p = Pattern.compile( prefixPattern + "(" +
					"([a-zA-Z0-9_]+)|" + // Normal identifiers
					// Ignore single quoted identifiers to avoid possible clashes with string literals
					// and since SQLLite is the only DB supporting that style, we simply decide to not support it
					//"('[a-zA-Z0-9_]+'((''[a-zA-Z0-9_]+)+')?)|" + // Single quoted identifiers
					"(\"[a-zA-Z0-9_]+\"((\"\"[a-zA-Z0-9_]+)+\")?)|" + // Double quoted identifiers
					"(`[a-zA-Z0-9_]+`((``[a-zA-Z0-9_]+)+`)?)|" + // MySQL quoted identifiers
					"(\\[[a-zA-Z0-9_\\s]+\\])" + // MSSQL quoted identifiers
				")"
			);
			Matcher matcher = p.matcher( withClauseFragment );
			StringBuilder withClauseSb = new StringBuilder( withClauseFragment.length() );
			withClauseSb.append( " and " );

			int start = 0;
			int aliasNumber = 0;
			while ( matcher.find() ) {
				final String matchedTableName = matcher.group(1);
				final String column = matcher.group( 2 );
				// Replace non-valid simple identifier characters from the column name
				final String alias = "c_" + aliasNumber + "_" + column.replaceAll( "[\\[\\]\\s\"']+", "" );
				withClauseSb.append( withClauseFragment, start, matcher.start() );
				withClauseSb.append( first.getAlias() );
				withClauseSb.append( '.' );
				withClauseSb.append( alias );
				withClauseSb.append( ' ' );

				subqueryJoinFragment.addFromFragmentString( ", " );
				subqueryJoinFragment.addFromFragmentString( matchedTableName );
				subqueryJoinFragment.addFromFragmentString( "." );
				subqueryJoinFragment.addFromFragmentString( column );
				subqueryJoinFragment.addFromFragmentString( " as " );
				subqueryJoinFragment.addFromFragmentString( alias );

				start = matcher.end();
				aliasNumber++;
			}

			withClauseSb.append( withClauseFragment, start, withClauseFragment.length() );

			subqueryJoinFragment.addFromFragmentString( " from " );
			subqueryJoinFragment.addFromFragmentString( first.joinable.getTableName() );
			subqueryJoinFragment.addFromFragmentString( " " );
			subqueryJoinFragment.addFromFragmentString( first.getAlias() );

			// Render following join sequences in a sub-sequence
			JoinSequence subSequence = new JoinSequence( factory );

			while ( iter.hasNext() ) {
				Join join = iter.next();
				subSequence.joins.add( join );
			}

			JoinFragment subFragment = subSequence.toJoinFragment(
					enabledFilters,
					false,
					true, // TODO: only join subclasses that are needed for ON clause
					null,
					null,
					null
			);
			subqueryJoinFragment.addFragment( subFragment );
			subqueryJoinFragment.addFromFragmentString( ")" );

			joinFragment.addJoin(
					subqueryJoinFragment.toFromFragmentString(),
					first.getAlias(),
					first.getLHSColumns(),
					JoinHelper.getRHSColumnNames( first.getAssociationType(), factory ),
					first.joinType,
					withClauseSb.toString()
			);

			for ( Join join : joins ) {
				// Skip joining the first join node as it is contained in the subquery
				if ( join != first ) {
					joinFragment.addJoin(
							join.getJoinable().getTableName(),
							join.getAlias(),
							join.getLHSColumns(),
							JoinHelper.getRHSColumnNames( join.getAssociationType(), factory ),
							join.joinType,
							""
					);
				}
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
