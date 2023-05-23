/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.Map;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * SqlAstProcessingState specialization for query parts
 *
 * @author Steve Ebersole
 */
public interface SqlAstQueryPartProcessingState extends SqlAstProcessingState {
	/**
	 * Get the QueryPart being processed as part of this state.  It is
	 * considered in-flight as it is probably still being built.
	 */
	QueryPart getInflightQueryPart();

	/**
	 * Registers that the given SqmFrom is treated.
	 */
	void registerTreatedFrom(SqmFrom<?, ?> sqmFrom);

	/**
	 * Registers that the given SqmFrom was used in an expression and whether to downgrade {@link org.hibernate.persister.entity.EntityNameUse#TREAT} of it.
	 */
	void registerFromUsage(SqmFrom<?, ?> sqmFrom, boolean downgradeTreatUses);

	/**
	 * Returns the treated SqmFroms and whether their {@link org.hibernate.persister.entity.EntityNameUse#TREAT}
	 * should be downgraded to {@link org.hibernate.persister.entity.EntityNameUse#EXPRESSION}.
	 */
	Map<SqmFrom<?, ?>, Boolean> getFromRegistrations();
}
