/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Things that can have {@link org.hibernate.annotations.Where},
 * and/or {@link org.hibernate.annotations.Filter} applied to them.
 * This is effectively {@linkplain EntityMappingType entities} and
 * {@linkplain PluralAttributeMapping plural attributes}.
 */
public interface Restrictable extends FilterRestrictable, WhereRestrictable {
	/**
	 * Applies the base set of restrictions.
	 * @deprecated Use {@link #applyBaseRestrictions(Consumer, TableGroup, boolean, Map, boolean, Set, SqlAstCreationState)} instead
	 */
	@Deprecated(forRemoval = true)
	default void applyBaseRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
		applyBaseRestrictions(
				predicateConsumer,
				tableGroup,
				useQualifier,
				enabledFilters,
				false,
				treatAsDeclarations,
				creationState
		);
	}

	/**
	 * Applies the base set of restrictions.
	 */
	void applyBaseRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState);
}
