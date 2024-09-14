/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Extension to MutationOperation for cases where the operation wants to
 * handle execution itself.
 *
 * @author Steve Ebersole
 */
public interface SelfExecutingUpdateOperation extends MutationOperation {
	void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session);

}
