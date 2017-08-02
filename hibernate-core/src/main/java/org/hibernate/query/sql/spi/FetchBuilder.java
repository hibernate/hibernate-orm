/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NativeQuery;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.exec.results.spi.Fetch;
import org.hibernate.sql.exec.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class FetchBuilder implements NativeQuery.FetchReturn {
	private final String tableAlias;
	private final String fetchParentTableAlias;
	private final String joinPropertyName;

	public FetchBuilder(String tableAlias, String fetchParentTableAlias, String joinPropertyName) {
		this.tableAlias = tableAlias;
		this.fetchParentTableAlias = fetchParentTableAlias;
		this.joinPropertyName = joinPropertyName;
	}

	public Fetch buildFetch(BuilderExecutionState builderExecutionState, NodeResolutionContext resolutionContext) {
		final FetchParent fetchParent = builderExecutionState.getFetchParentByParentAlias( fetchParentTableAlias );
		if ( fetchParent == null ) {
			throw new HibernateException( "FetchParent for table-alias [" + fetchParentTableAlias + "] not yet resolved" );
		}

		final Navigable joinedNavigable = fetchParent.getNavigableContainerReference().getNavigable().findNavigable( joinPropertyName );
		if ( joinedNavigable == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not locate attribute/navigable for given name join name [%s] relative to container [%s (%s)]",
							joinPropertyName,
							fetchParent.getNavigableContainerReference().getNavigable().asLoggableText(),
							fetchParentTableAlias
					)

			);
		}

		assert joinedNavigable instanceof Fetchable;

		( (Fetchable) joinedNavigable ).generateFetch(
				fetchParent,
				fetchParent.getNavigableContainerReference(),
				tableAlias,
				resolveSqlSelectionMap(),
				resolutionContext
		);
		return fetchParent;
	}

	private void resolveSqlSelectionMap() {

	}
}
