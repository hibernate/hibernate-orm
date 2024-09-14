/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.select;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.query.sqm.SetOperator;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Christian Beikov
 */
public class QueryGroup extends QueryPart {
	private final SetOperator setOperator;
	private final List<QueryPart> queryParts;

	public QueryGroup(boolean isRoot, SetOperator setOperator, List<QueryPart> queryParts) {
		super( isRoot );
		this.setOperator = setOperator;
		this.queryParts = queryParts;
	}

	@Override
	public QuerySpec getFirstQuerySpec() {
		return queryParts.get( 0 ).getFirstQuerySpec();
	}

	@Override
	public QuerySpec getLastQuerySpec() {
		return queryParts.get( queryParts.size() - 1 ).getLastQuerySpec();
	}

	@Override
	public void visitQuerySpecs(Consumer<QuerySpec> querySpecConsumer) {
		for ( int i = 0; i < queryParts.size(); i++ ) {
			queryParts.get( i ).visitQuerySpecs( querySpecConsumer );
		}
	}

	@Override
	public <T> T queryQuerySpecs(Function<QuerySpec, T> querySpecConsumer) {
		for ( int i = 0; i < queryParts.size(); i++ ) {
			T result = queryParts.get( i ).queryQuerySpecs( querySpecConsumer );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	public SetOperator getSetOperator() {
		return setOperator;
	}

	public List<QueryPart> getQueryParts() {
		return queryParts;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQueryGroup( this );
	}

}
