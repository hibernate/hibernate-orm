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
package org.hibernate.hql.ast.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.QueryTranslatorImpl;
import org.hibernate.hql.ast.tree.FromClause;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.hql.ast.tree.QueryNode;
import org.hibernate.hql.ast.tree.DotNode;
import org.hibernate.sql.JoinFragment;
import org.hibernate.util.StringHelper;

import antlr.ASTFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs the post-processing of the join information gathered during semantic analysis.
 * The join generating classes are complex, this encapsulates some of the JoinSequence-related
 * code.
 *
 * @author Joshua Davis
 */
public class JoinProcessor implements SqlTokenTypes {

	private static final Logger log = LoggerFactory.getLogger( JoinProcessor.class );

	private QueryTranslatorImpl queryTranslatorImpl;
	private SyntheticAndFactory andFactory;

	/**
	 * Constructs a new JoinProcessor.
	 *
	 * @param astFactory		  The factory for AST node creation.
	 * @param queryTranslatorImpl The query translator.
	 */
	public JoinProcessor(ASTFactory astFactory, QueryTranslatorImpl queryTranslatorImpl) {
		this.andFactory = new SyntheticAndFactory( astFactory );
		this.queryTranslatorImpl = queryTranslatorImpl;
	}

	/**
	 * Translates an AST join type (i.e., the token type) into a JoinFragment.XXX join type.
	 *
	 * @param astJoinType The AST join type (from HqlSqlTokenTypes or SqlTokenTypes)
	 * @return a JoinFragment.XXX join type.
	 * @see JoinFragment
	 * @see SqlTokenTypes
	 */
	public static int toHibernateJoinType(int astJoinType) {
		switch ( astJoinType ) {
			case LEFT_OUTER:
				return JoinFragment.LEFT_OUTER_JOIN;
			case INNER:
				return JoinFragment.INNER_JOIN;
			case RIGHT_OUTER:
				return JoinFragment.RIGHT_OUTER_JOIN;
			default:
				throw new AssertionFailure( "undefined join type " + astJoinType );
		}
	}

	public void processJoins(QueryNode query, boolean inSubquery) {
		final FromClause fromClause = query.getFromClause();

		final List fromElements;
		if ( DotNode.useThetaStyleImplicitJoins ) {
			// for regression testing against output from the old parser...
			// found it easiest to simply reorder the FromElements here into ascending order
			// in terms of injecting them into the resulting sql ast in orders relative to those
			// expected by the old parser; this is definitely another of those "only needed
			// for regression purposes".  The SyntheticAndFactory, then, simply injects them as it
			// encounters them.
			fromElements = new ArrayList();
			ListIterator liter = fromClause.getFromElements().listIterator( fromClause.getFromElements().size() );
			while ( liter.hasPrevious() ) {
				fromElements.add( liter.previous() );
			}
		}
		else {
			fromElements = fromClause.getFromElements();
		}

		// Iterate through the alias,JoinSequence pairs and generate SQL token nodes.
		Iterator iter = fromElements.iterator();
		while ( iter.hasNext() ) {
			final FromElement fromElement = ( FromElement ) iter.next();
			JoinSequence join = fromElement.getJoinSequence();
			join.setSelector(
					new JoinSequence.Selector() {
						public boolean includeSubclasses(String alias) {
							// The uber-rule here is that we need to include  subclass joins if
							// the FromElement is in any way dereferenced by a property from
							// the subclass table; otherwise we end up with column references
							// qualified by a non-existent table reference in the resulting SQL...
							boolean containsTableAlias = fromClause.containsTableAlias( alias );
							if ( fromElement.isDereferencedBySubclassProperty() ) {
								// TODO : or should we return 'containsTableAlias'??
								log.trace( "forcing inclusion of extra joins [alias=" + alias + ", containsTableAlias=" + containsTableAlias + "]" );
								return true;
							}
							boolean shallowQuery = queryTranslatorImpl.isShallowQuery();
							boolean includeSubclasses = fromElement.isIncludeSubclasses();
							boolean subQuery = fromClause.isSubQuery();
							return includeSubclasses && containsTableAlias && !subQuery && !shallowQuery;
						}
					}
			);
			addJoinNodes( query, join, fromElement, inSubquery );
		}

	}

	private void addJoinNodes(QueryNode query, JoinSequence join, FromElement fromElement, boolean inSubquery) {
		// Generate FROM and WHERE fragments for the from element.
		JoinFragment joinFragment = join.toJoinFragment(
				inSubquery ? Collections.EMPTY_MAP : queryTranslatorImpl.getEnabledFilters(),
				fromElement.useFromFragment() || fromElement.isDereferencedBySuperclassOrSubclassProperty(),
				fromElement.getWithClauseFragment(),
				fromElement.getWithClauseJoinAlias()
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
		if ( fromElement.useFromFragment() /*&& StringHelper.isNotEmpty( frag )*/ ) {
			String fromFragment = processFromFragment( frag, join );
			if ( log.isDebugEnabled() ) {
				log.debug( "Using FROM fragment [" + fromFragment + "]" );
			}
			fromElement.setText( fromFragment.trim() ); // Set the text of the fromElement.
		}
		andFactory.addWhereFragment( joinFragment, whereFrag, query, fromElement );
	}

	private String processFromFragment(String frag, JoinSequence join) {
		String fromFragment = frag.trim();
		// The FROM fragment will probably begin with ', '.  Remove this if it is present.
		if ( fromFragment.startsWith( ", " ) ) {
			fromFragment = fromFragment.substring( 2 );
		}
		return fromFragment;
	}

}
