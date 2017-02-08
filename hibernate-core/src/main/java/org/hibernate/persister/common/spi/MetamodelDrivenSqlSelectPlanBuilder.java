/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.sql.convert.spi.SqlSelectPlan;

/**
 * Specialization {@link NavigableVisitationStrategy} implementation for
 * building {@link SqlSelectPlan} instances driven primarily by mapping
 * metadata.
 *
 * @author Steve Ebersole
 */
public interface MetamodelDrivenSqlSelectPlanBuilder extends NavigableVisitationStrategy {
	/**
	 * Build the SqlSelectPlan, driven by mapping model, with the given NavigableSource as query root
	 * leveraging the NavigableVisitationStrategy visitor contract.  The mapping model along with the
	 * passed parameters indicate the shape of the SelectQuery AST as well as the shape of the results
	 * as indicated by the query Return graphs
	 *
	 * @param rootNavigable The NavigableSource which is the root of the query.
	 *
	 * @return The SqlSelectPlan
	 */
	SqlSelectPlan buildSqlSelectPlan(NavigableSource rootNavigable);
}
