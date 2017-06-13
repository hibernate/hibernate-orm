/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.hql.spi.id;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.sql.ast.produce.sqm.internal.IdSelectGenerator;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class IdTableBasedUpdateHandler implements MultiTableBulkIdStrategy.UpdateHandler {
	private final QuerySpec entityIdSelection;

	private final Predicate idInIdTableRestriction;

	public IdTableBasedUpdateHandler(
			EntityDescriptor entityToUpdate,
			SqmUpdateStatement sqmUpdate,
			QueryOptions queryOptions,
			SessionFactoryImplementor factory) {
		// we will need:
		//		1) the INSERT-SELECT for saving off the matching ids
		//		2) one or more UPDATE statements based on the SQM assignment
		// 			specs, including splitting the individual assignments
		//			to the proper physical UPDATE.  Each is restricted
		//			based on the ids saved in (1)
		//		3) possibly a DELETE from the id table populated in (1)

		// NOTE that the count returned from the INSERT-SELECT is the number
		//		we ultimately report as the result from Query#executeUpdate...

		// build the SELECT portion of the INSERT-SELECT
		entityIdSelection = IdSelectGenerator.generateEntityIdSelect(
				entityToUpdate,
				sqmUpdate,
				queryOptions,
				factory
		);

		idInIdTableRestriction =
	}

	@Override
	public int execute(
			QueryParameterBindings parameterBindings,
			SharedSessionContractImplementor session) {
		return 0;
	}
}
