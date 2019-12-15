/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DotNode;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.FromReferenceNode;
import org.hibernate.hql.internal.ast.tree.ImpliedFromElement;
import org.hibernate.hql.internal.ast.tree.ParameterContainer;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.SqlFragment;
import org.hibernate.hql.internal.ast.tree.TableReferenceNode;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.param.DynamicFilterParameterSpecification;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.Type;

import antlr.collections.AST;

/**
 * Performs the post-processing of the join information gathered during semantic analysis.
 * The join generating classes are complex, this encapsulates some of the JoinSequence-related
 * code.
 *
 * @author Joshua Davis
 */
public class JoinProcessor implements SqlTokenTypes {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JoinProcessor.class );

	private static final Pattern DYNAMIC_FILTER_PATTERN = Pattern.compile(":(\\w+\\S*)\\s");
	private static final String LITERAL_DELIMITER = "'";

	private final HqlSqlWalker walker;
	private final SyntheticAndFactory syntheticAndFactory;

	/**
	 * Constructs a new JoinProcessor.
	 *
	 * @param walker The walker to which we are bound, giving us access to needed resources.
	 */
	public JoinProcessor(HqlSqlWalker walker) {
		this.walker = walker;
		this.syntheticAndFactory = new SyntheticAndFactory( walker );
	}

	/**
	 * Translates an AST join type (i.e., the token type) into a JoinFragment.XXX join type.
	 *
	 * @param astJoinType The AST join type (from HqlSqlTokenTypes or SqlTokenTypes)
	 *
	 * @return a JoinFragment.XXX join type.
	 *
	 * @see JoinFragment
	 * @see SqlTokenTypes
	 */
	public static JoinType toHibernateJoinType(int astJoinType) {
		switch ( astJoinType ) {
			case LEFT_OUTER: {
				return JoinType.LEFT_OUTER_JOIN;
			}
			case INNER: {
				return JoinType.INNER_JOIN;
			}
			case RIGHT_OUTER: {
				return JoinType.RIGHT_OUTER_JOIN;
			}
			case FULL: {
				return JoinType.FULL_JOIN;
			}
			default: {
				throw new AssertionFailure( "undefined join type " + astJoinType );
			}
		}
	}

	private Set<String> findQueryReferencedTables(QueryNode query) {
		if ( !walker.getSessionFactoryHelper()
				.getFactory()
				.getSessionFactoryOptions()
				.isOmitJoinOfSuperclassTablesEnabled() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( String.format(
						"Finding of query referenced tables is skipped because the feature is disabled. See %s",
						AvailableSettings.OMIT_JOIN_OF_SUPERCLASS_TABLES
				) );
			}
			return null;
		}

		if ( CollectionHelper.isNotEmpty( walker.getEnabledFilters() ) ) {
			LOG.debug( "Finding of query referenced tables is skipped because filters are enabled." );
			return null;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debug( TokenPrinters.REFERENCED_TABLES_PRINTER.showAsString(
					query,
					"Tables referenced from query nodes:"
			) );
		}

		Set<String> result = new HashSet<>();

		// Find tables referenced by FromReferenceNodes
		collectReferencedTables( new ASTIterator( query ), result );
		for (FromElement fromElement : (List<FromElement>) query.getFromClause().getFromElements()) {
			// For joins, we want to add the table where the association key is mapped as well as that could be a supertype that we need to join
			String role = fromElement.getRole();
			if ( role != null ) {
				result.add( fromElement.getOrigin().getPropertyTableName(role.substring(role.lastIndexOf('.') + 1)) );
			}
			AST withClauseAst = fromElement.getWithClauseAst();
			if ( withClauseAst != null ) {
				collectReferencedTables( new ASTIterator( withClauseAst ), result );
			}
		}


		// Find tables referenced by fromElementsForLoad
		if ( query.getSelectClause() != null ) {
			for ( Object element : query.getSelectClause().getFromElementsForLoad() ) {
				FromElement fromElement = (FromElement) element;
				EntityPersister entityPersister = fromElement.getEntityPersister();
				if ( entityPersister != null && entityPersister instanceof AbstractEntityPersister ) {
					AbstractEntityPersister aep = (AbstractEntityPersister) entityPersister;
					String[] tables = aep.getTableNames();
					Collections.addAll(result, tables);
				}
			}
		}

		return result;
	}

	private void collectReferencedTables(ASTIterator iterator, Set<String> result) {
		while ( iterator.hasNext() ) {
			AST node = iterator.nextNode();
			if ( node instanceof TableReferenceNode) {
				TableReferenceNode fromReferenceNode = (TableReferenceNode) node;
				String[] tables = fromReferenceNode.getReferencedTables();
				if ( tables != null ) {
					Collections.addAll(result, tables);
				}
			}
			if (node instanceof SqlFragment) {
				SqlFragment sqlFragment = (SqlFragment) node;
				FromElement fromElement = sqlFragment.getFromElement();

				if (fromElement != null) {
					// For joins, we want to add the table where the association key is mapped as well as that could be a supertype that we need to join
					String role = fromElement.getRole();
					if ( role != null ) {
						result.add( fromElement.getOrigin().getPropertyTableName(role.substring(role.lastIndexOf('.') + 1)) );
					}
					AST withClauseAst = fromElement.getWithClauseAst();
					if ( withClauseAst != null ) {
						collectReferencedTables( new ASTIterator( withClauseAst ), result );
					}
				}

			}
		}
	}

	public void processJoins(QueryNode query) {
		final FromClause fromClause = query.getFromClause();

		Set<String> queryReferencedTables = findQueryReferencedTables( query );

		final List fromElements;
		if ( DotNode.useThetaStyleImplicitJoins ) {
			// for regression testing against output from the old parser...
			// found it easiest to simply reorder the FromElements here into ascending order
			// in terms of injecting them into the resulting sql ast in orders relative to those
			// expected by the old parser; this is definitely another of those "only needed
			// for regression purposes". The SyntheticAndFactory, then, simply injects them as it
			// encounters them.
			fromElements = new ArrayList();
			ListIterator liter = fromClause.getFromElements().listIterator( fromClause.getFromElements().size() );
			while ( liter.hasPrevious() ) {
				fromElements.add( liter.previous() );
			}
		}
		else {
			fromElements = new ArrayList( fromClause.getFromElements().size() );
			ListIterator<FromElement> liter = fromClause.getFromElements().listIterator();
			while ( liter.hasNext() ) {
				FromElement fromElement = liter.next();

				// We found an implied from element that is used in the WITH clause of another from element, so it need to become part of it's join sequence
				if ( fromElement instanceof ImpliedFromElement
						&& fromElement.getOrigin().getWithClauseFragment() != null
						&& fromElement.getOrigin().getWithClauseFragment().contains( fromElement.getTableAlias() ) ) {
					fromElement.getOrigin().getJoinSequence().addJoin( (ImpliedFromElement) fromElement );
					// This from element will be rendered as part of the origins join sequence
					fromElement.setText( "" );
				}
				else {
					fromElements.add( fromElement );
				}
			}
		}

		// Iterate through the alias,JoinSequence pairs and generate SQL token nodes.
		Iterator iter = fromElements.iterator();
		while ( iter.hasNext() ) {
			final FromElement fromElement = (FromElement) iter.next();
			JoinSequence join = fromElement.getJoinSequence();
			join.setQueryReferencedTables( queryReferencedTables );
			join.setSelector(
					new JoinSequence.Selector() {
						public boolean includeSubclasses(String alias) {
							// The uber-rule here is that we need to include subclass joins if
							// the FromElement is in any way dereferenced by a property from
							// the subclass table; otherwise we end up with column references
							// qualified by a non-existent table reference in the resulting SQL...
							boolean containsTableAlias = fromClause.containsTableAlias( alias );
							if ( fromElement.isDereferencedBySubclassProperty() ) {
								// TODO : or should we return 'containsTableAlias'??
								LOG.tracev(
										"Forcing inclusion of extra joins [alias={0}, containsTableAlias={1}]",
										alias,
										containsTableAlias
								);
								return true;
							}
							boolean shallowQuery = walker.isShallowQuery();
							boolean includeSubclasses = fromElement.isIncludeSubclasses();
							boolean subQuery = fromClause.isSubQuery();
							return includeSubclasses && containsTableAlias && !subQuery && !shallowQuery;
						}
					}
			);
			addJoinNodes( query, join, fromElement );
		}

	}

	private void addJoinNodes(QueryNode query, JoinSequence join, FromElement fromElement) {
		JoinFragment joinFragment = join.toJoinFragment(
				walker.getEnabledFilters(),
				fromElement.useFromFragment() || fromElement.isDereferencedBySuperclassOrSubclassProperty(),
				fromElement.getWithClauseFragment()
		);

		String frag = joinFragment.toFromFragmentString();
		String whereFrag = joinFragment.toWhereFragmentString();

		// If the from element represents a JOIN_FRAGMENT and it is
		// a theta-style join, convert its type from JOIN_FRAGMENT
		// to FROM_FRAGMENT
		if ( fromElement.getType() == JOIN_FRAGMENT &&
				( join.isThetaStyle() || StringHelper.isNotEmpty( whereFrag ) ) ) {
			fromElement.setType( FROM_FRAGMENT );
			fromElement.getJoinSequence().setUseThetaStyle( true ); // this is used during SqlGenerator processing
		}

		// If there is a FROM fragment and the FROM element is an explicit, then add the from part.
		if ( fromElement.useFromFragment() ||
				( fromElement.getFromClause().isSubQuery()
						&& fromElement.isDereferencedBySuperclassOrSubclassProperty() ) /*&& StringHelper.isNotEmpty( frag )*/ ) {
			String fromFragment = processFromFragment( frag, join ).trim();
			LOG.debugf( "Using FROM fragment [%s]", fromFragment );
			processDynamicFilterParameters(
					fromFragment,
					fromElement,
					walker
			);
		}

		syntheticAndFactory.addWhereFragment(
				joinFragment,
				whereFrag,
				query,
				fromElement,
				walker
		);
	}

	private String processFromFragment(String frag, JoinSequence join) {
		String fromFragment = frag.trim();
		// The FROM fragment will probably begin with ', '. Remove this if it is present.
		if ( fromFragment.startsWith( ", " ) ) {
			fromFragment = fromFragment.substring( 2 );
		}
		return fromFragment;
	}

	public static void processDynamicFilterParameters(
			final String sqlFragment,
			final ParameterContainer container,
			final HqlSqlWalker walker) {
		if ( walker.getEnabledFilters().isEmpty()
				&& ( !hasDynamicFilterParam( walker, sqlFragment ) )
				&& ( !( hasCollectionFilterParam( sqlFragment ) ) ) ) {
			return;
		}

		Dialect dialect = walker.getDialect();
		String symbols = ParserHelper.HQL_SEPARATORS + dialect.openQuote() + dialect.closeQuote();
		StringTokenizer tokens = new StringTokenizer( sqlFragment, symbols, true );
		StringBuilder result = new StringBuilder();

		while ( tokens.hasMoreTokens() ) {
			final String token = tokens.nextToken();
			if ( token.startsWith( ParserHelper.HQL_VARIABLE_PREFIX ) ) {
				final String filterParameterName = token.substring( 1 );
				final String[] parts = LoadQueryInfluencers.parseFilterParameterName( filterParameterName );
				final FilterImpl filter = (FilterImpl) walker.getEnabledFilters().get( parts[0] );
				final Object value = filter.getParameter( parts[1] );
				final Type type = filter.getFilterDefinition().getParameterType( parts[1] );
				final String typeBindFragment = String.join(
						",",
						ArrayHelper.fillArray(
								"?",
								type.getColumnSpan( walker.getSessionFactoryHelper().getFactory() )
						)
				);
				final String bindFragment;
				if ( value != null && Collection.class.isInstance( value ) ) {
					bindFragment = String.join(
							",",
							ArrayHelper.fillArray( typeBindFragment, ( (Collection) value ).size() )
					);
				}
				else {
					bindFragment = typeBindFragment;
				}
				result.append( bindFragment );
				container.addEmbeddedParameter( new DynamicFilterParameterSpecification( parts[0], parts[1], type ) );
			}
			else {
				result.append( token );
			}
		}

		container.setText( result.toString() );
	}

	private static boolean hasDynamicFilterParam(HqlSqlWalker walker, String sqlFragment) {
		String closeQuote = String.valueOf( walker.getDialect().closeQuote()  );

		Matcher matcher = DYNAMIC_FILTER_PATTERN.matcher( sqlFragment );
		if ( matcher.find() && matcher.groupCount() > 0 ) {
			String match = matcher.group( 1 );
			return match.endsWith( closeQuote ) || match.endsWith( LITERAL_DELIMITER );
		}
		return true;
	}

	private static boolean hasCollectionFilterParam(String sqlFragment) {
		return !sqlFragment.contains( "?" );
	}
}
