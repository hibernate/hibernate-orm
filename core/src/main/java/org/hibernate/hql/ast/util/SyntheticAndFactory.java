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

import java.util.Map;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.hql.ast.tree.QueryNode;
import org.hibernate.hql.ast.tree.RestrictableStatement;
import org.hibernate.hql.ast.tree.SqlFragment;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.util.StringHelper;

import antlr.ASTFactory;
import antlr.collections.AST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates synthetic and nodes based on the where fragment part of a JoinSequence.
 *
 * @author josh
 */
public class SyntheticAndFactory implements HqlSqlTokenTypes {
	private static final Logger log = LoggerFactory.getLogger( SyntheticAndFactory.class );

	private ASTFactory astFactory;
	private AST thetaJoins;
	private AST filters;

	public SyntheticAndFactory(ASTFactory astFactory) {
		this.astFactory = astFactory;
	}

	public void addWhereFragment(JoinFragment joinFragment, String whereFragment, QueryNode query, FromElement fromElement) {

		if ( whereFragment == null ) {
			return;
		}

		whereFragment = whereFragment.trim();
		if ( StringHelper.isEmpty( whereFragment ) ) {
			return;
		}
		else if ( !fromElement.useWhereFragment() && !joinFragment.hasThetaJoins() ) {
			return;
		}

		// Forcefully remove leading ands from where fragments; the grammar will
		// handle adding them
		if ( whereFragment.startsWith( "and" ) ) {
			whereFragment = whereFragment.substring( 4 );
		}

		if ( log.isDebugEnabled() ) log.debug( "Using WHERE fragment [" + whereFragment + "]" );

		SqlFragment fragment = ( SqlFragment ) ASTUtil.create( astFactory, SQL_TOKEN, whereFragment );
		fragment.setJoinFragment( joinFragment );
		fragment.setFromElement( fromElement );

		// Filter conditions need to be inserted before the HQL where condition and the
		// theta join node.  This is because org.hibernate.loader.Loader binds the filter parameters first,
		// then it binds all the HQL query parameters, see org.hibernate.loader.Loader.processFilterParameters().
		if ( fragment.getFromElement().isFilter() || fragment.hasFilterCondition() ) {
			if ( filters == null ) {
				// Find or create the WHERE clause
				AST where = query.getWhereClause();
				// Create a new FILTERS node as a parent of all filters
				filters = astFactory.create( FILTERS, "{filter conditions}" );
				// Put the FILTERS node before the HQL condition and theta joins
				ASTUtil.insertChild( where, filters );
			}
			
			// add the current fragment to the FILTERS node
			filters.addChild( fragment );
		}
		else {
			if ( thetaJoins == null ) {
				// Find or create the WHERE clause
				AST where = query.getWhereClause();
				// Create a new THETA_JOINS node as a parent of all filters
				thetaJoins = astFactory.create( THETA_JOINS, "{theta joins}" );
				// Put the THETA_JOINS node before the HQL condition, after the filters.
				if (filters==null) {
					ASTUtil.insertChild( where, thetaJoins );
				}
				else {
					ASTUtil.insertSibling( thetaJoins, filters );
				}
			}
			
			// add the current fragment to the THETA_JOINS node
			thetaJoins.addChild(fragment);
		}

	}

	public void addDiscriminatorWhereFragment(RestrictableStatement statement, Queryable persister, Map enabledFilters, String alias) {
		String whereFragment = persister.filterFragment( alias, enabledFilters ).trim();
		if ( "".equals( whereFragment ) ) {
			return;
		}
		if ( whereFragment.startsWith( "and" ) ) {
			whereFragment = whereFragment.substring( 4 );
		}

		// Need to parse off the column qualifiers; this is assuming (which is true as of now)
		// that this is only used from update and delete HQL statement parsing
		whereFragment = StringHelper.replace( whereFragment, persister.generateFilterConditionAlias( alias ) + ".", "" );

		// Note: this simply constructs a "raw" SQL_TOKEN representing the
		// where fragment and injects this into the tree.  This "works";
		// however it is probably not the best long-term solution.
		//
		// At some point we probably want to apply an additional grammar to
		// properly tokenize this where fragment into constituent parts
		// focused on the operators embedded within the fragment.
		AST discrimNode = astFactory.create( SQL_TOKEN, whereFragment );

		if ( statement.getWhereClause().getNumberOfChildren() == 0 ) {
			statement.getWhereClause().setFirstChild( discrimNode );
		}
		else {
			AST and = astFactory.create( AND, "{and}" );
			AST currentFirstChild = statement.getWhereClause().getFirstChild();
			and.setFirstChild( discrimNode );
			and.addChild( currentFirstChild );
			statement.getWhereClause().setFirstChild( and );
		}
	}
}
