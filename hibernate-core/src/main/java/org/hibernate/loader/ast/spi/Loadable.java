/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.spi;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.from.RootTableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Contract for things that can be loaded by a Loader.
 *
 * Generally speaking this is limited to entities and collections
 *
 * @see Loader
 *
 * @author Steve Ebersole
 */
public interface Loadable extends ModelPart, RootTableGroupProducer {
	boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers);
	boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers);
	boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers);

	String getRootPathName();

	@Override
	default TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState, SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	default TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState, SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
