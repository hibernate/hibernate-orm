/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;
import java.util.Locale;

import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;

/**
 * @author Steve Ebersole
 */
public class MutationOperationGroupStandard extends AbstractMutationOperationGroup {
	private final List<MutationOperation> operations;

	public MutationOperationGroupStandard(MutationType mutationType, MutationTarget<?> mutationTarget, List<MutationOperation> operations) {
		super( mutationType, mutationTarget );
		this.operations = operations;
	}

	@Override
	public int getNumberOfOperations() {
		return operations.size();
	}

	@Override
	public MutationOperation getSingleOperation() {
		if ( operations.size() == 1 ) {
			return operations.get( 0 );
		}
		throw new IllegalStateException(
				String.format(
						Locale.ROOT,
						"Group contains multiple table mutations - %s : %s ",
						getMutationType().name(),
						getMutationTarget().getNavigableRole()
				)
		);
	}

	@Override
	public MutationOperation getOperation(int idx) {
		return operations.get( idx );
	}

	@Override
	public MutationOperation getOperation(String tableName) {
		for ( int i = 0; i < operations.size(); i++ ) {
			final MutationOperation operation = operations.get( i );
			if ( operation.getTableDetails().getTableName().equals( tableName ) ) {
				return operation;
			}
		}
		return null;
	}

}
