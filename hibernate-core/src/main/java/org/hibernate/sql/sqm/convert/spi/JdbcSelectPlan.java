/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.spi;

import java.util.List;

import org.hibernate.loader.plan.spi.Return;

/**
 * JdbcOperationPlan specific to performing a {@code SELECT} query.  Tha main difference is the
 * added definition of how to extract results from the query results.
 *
 * @author Steve Ebersole
 */
public interface JdbcSelectPlan extends JdbcOperationPlan {
	/**
	 * LoadPlan was designed to be a PoC of this "SQM" work, but built around the limitations in
	 * upstream ORM.  Moving forward, this just needs to be some form of "return" descriptors.  For now
	 * just use the LoadPlan notion of a Return.  Expect the return type to change or for Return to be
	 * changed as we continue SQM dev.
	 *
	 * @return
	 */
	List<Return> getReturns();
}
