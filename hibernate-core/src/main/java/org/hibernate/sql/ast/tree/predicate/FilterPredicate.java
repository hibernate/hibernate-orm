/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.List;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * Represents a filter applied to an entity/collection.
 * <p/>
 * Note, we do not attempt to parse the filter
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class FilterPredicate implements Predicate {
	private final String filterFragment;
	private final List<FilterJdbcParameter> filterJdbcParameters;

	public FilterPredicate(String filterFragment, List<FilterJdbcParameter> filterJdbcParameters) {
		this.filterFragment = filterFragment;
		this.filterJdbcParameters = filterJdbcParameters;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitFilterPredicate( this );
	}

	public String getFilterFragment() {
		return filterFragment;
	}

	public List<FilterJdbcParameter> getFilterJdbcParameters() {
		return filterJdbcParameters;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return null;
	}
}
