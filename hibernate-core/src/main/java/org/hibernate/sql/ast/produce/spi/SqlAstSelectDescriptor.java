/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.List;

import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.results.spi.DomainResult;

/**
 * SqlAstDescriptor specialization for select queries
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface SqlAstSelectDescriptor extends SqlAstDescriptor {
	@Override
	SelectStatement getSqlAstStatement();

	/**
	 * The descriptors for how to process results
	 */
	List<DomainResult> getQueryResults();
}
