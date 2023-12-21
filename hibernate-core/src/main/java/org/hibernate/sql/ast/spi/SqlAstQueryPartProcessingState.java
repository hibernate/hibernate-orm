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
public interface SqlAstQueryPartProcessingState extends SqlAstQueryNodeProcessingState {
	/**
	 * Get the QueryPart being processed as part of this state.  It is
	 * considered in-flight as it is probably still being built.
	 */
	QueryPart getInflightQueryPart();
}
