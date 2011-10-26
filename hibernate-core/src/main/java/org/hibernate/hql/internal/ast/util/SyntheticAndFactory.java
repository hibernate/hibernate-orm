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
package org.hibernate.hql.internal.ast.util;

import java.util.Map;

import antlr.collections.AST;
import org.jboss.logging.Logger;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.Node;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.RestrictableStatement;
import org.hibernate.hql.internal.ast.tree.SqlFragment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.CollectionFilterKeyParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.Type;

/**
 * Creates synthetic and nodes based on the where fragment part of a JoinSequence.
 *
 * @author josh
 */
public class SyntheticAndFactory implements HqlSqlTokenTypes {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SyntheticAndFactory.class.getName());

	private HqlSqlWalker hqlSqlWalker;
	private AST thetaJoins;
	private AST filters;

	public SyntheticAndFactory(HqlSqlWalker hqlSqlWalker) {
		this.hqlSqlWalker = hqlSqlWalker;
	}

	private Node create(int tokenType, String text) {
		return ( Node ) ASTUtil.create( hqlSqlWalker.getASTFactory(), tokenType, text );
	}

	public void addWhereFragment(
			JoinFragment joinFragment,
			String whereFragment,
			QueryNode query,
			FromElement fromElement,
			HqlSqlWalker hqlSqlWalker) {
		if ( whereFragment == null ) {
			return;
		}

		if ( !fromElement.useWhereFragment() && !joinFragment.hasThetaJoins() ) {
			return;
		}

		whereFragment = whereFragment.trim();
		if ( StringHelper.isEmpty( whereFragment ) ) {
			return;
		}

		// Forcefully remove leading ands from where fragments; the grammar will
		// handle adding them
		if ( whereFragment.startsWith( "and" ) ) {
			whereFragment = whereFragment.substring( 4 );
		}

		LOG.debugf( "Using unprocessed WHERE-fragment [%s]", whereFragment );

		SqlFragment fragment = ( SqlFragment ) create( SQL_TOKEN, whereFragment );
		fragment.setJoinFragment( joinFragment );
		fragment.setFromElement( fromElement );

		if ( fromElement.getIndexCollectionSelectorParamSpec() != null ) {
			fragment.addEmbeddedParameter( fromElement.getIndexCollectionSelectorParamSpec() );
			fromElement.setIndexCollectionSelectorParamSpec( null );
		}

		if ( hqlSqlWalker.isFilter() ) {
			if ( whereFragment.indexOf( '?' ) >= 0 ) {
				Type collectionFilterKeyType = hqlSqlWalker.getSessionFactoryHelper()
						.requireQueryableCollection( hqlSqlWalker.getCollectionFilterRole() )
						.getKeyType();
				CollectionFilterKeyParameterSpecification paramSpec = new CollectionFilterKeyParameterSpecification(
						hqlSqlWalker.getCollectionFilterRole(),
						collectionFilterKeyType,
						0
				);
				fragment.addEmbeddedParameter( paramSpec );
			}
		}

		JoinProcessor.processDynamicFilterParameters(
				whereFragment,
				fragment,
				hqlSqlWalker
		);

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Using processed WHERE-fragment [%s]", fragment.getText() );
		}

		// Filter conditions need to be inserted before the HQL where condition and the
		// theta join node.  This is because org.hibernate.loader.Loader binds the filter parameters first,
		// then it binds all the HQL query parameters, see org.hibernate.loader.Loader.processFilterParameters().
		if ( fragment.getFromElement().isFilter() || fragment.hasFilterCondition() ) {
			if ( filters == null ) {
				// Find or create the WHERE clause
				AST where = query.getWhereClause();
				// Create a new FILTERS node as a parent of all filters
				filters = create( FILTERS, "{filter conditions}" );
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
				thetaJoins = create( THETA_JOINS, "{theta joins}" );
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

	public void addDiscriminatorWhereFragment(
			RestrictableStatement statement,
			Queryable persister,
			Map enabledFilters,
			String alias) {
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
		SqlFragment discrimNode = ( SqlFragment ) create( SQL_TOKEN, whereFragment );

		JoinProcessor.processDynamicFilterParameters(
				whereFragment,
				discrimNode,
				hqlSqlWalker
		);

		if ( statement.getWhereClause().getNumberOfChildren() == 0 ) {
			statement.getWhereClause().setFirstChild( discrimNode );
		}
		else {
			AST and = create( AND, "{and}" );
			AST currentFirstChild = statement.getWhereClause().getFirstChild();
			and.setFirstChild( discrimNode );
			and.addChild( currentFirstChild );
			statement.getWhereClause().setFirstChild( and );
		}
	}
}
