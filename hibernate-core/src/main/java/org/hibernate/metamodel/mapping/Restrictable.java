/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Map;
import java.util.Set;

import org.hibernate.Filter;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * Things that can have {@link org.hibernate.annotations.Where}
 * and {@link org.hibernate.annotations.Filter} applied to them -
 * entities and collections
 */
public interface Restrictable {

	/**
	 * Apply {@link org.hibernate.annotations.Filter} and
	 * {@link org.hibernate.annotations.Where} restrictions
	 */
	void applyRestrictions(
			QuerySpec querySpec,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			FromClauseAccess fromClauseAccess);

	/**
	 * Apply {@link org.hibernate.annotations.Filter} and
	 * {@link org.hibernate.annotations.Where} restrictions
	 */
	void applyRestrictions(
			RestrictionPredicateConsumer predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			FromClauseAccess fromClauseAccess);

	enum RestrictionPredicatePartType {
		ENTITY,
		COLLECTION,
		MANY_TO_MANY,
		ONE_TO_MANY
	}

	enum RestrictionSourceType {
		/**
		 * {@link org.hibernate.annotations.Where}
		 */
		WHERE,
		/**
		 * {@link org.hibernate.annotations.Filter}
		 */
		FILTER
	}

	@FunctionalInterface
	interface RestrictionPredicateConsumer {
		void consume(Predicate predicate, Joinable.RestrictionPredicatePartType partType, Joinable.RestrictionSourceType sourceType);
	}
}
