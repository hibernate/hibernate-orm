/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMutationOperationGroup implements MutationOperationGroup {
	private final MutationType mutationType;
	private final MutationTarget<?> mutationTarget;

	public AbstractMutationOperationGroup(MutationType mutationType, MutationTarget<?> mutationTarget) {
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
	}

	@Override
	public MutationType getMutationType() {
		return mutationType;
	}

	@Override
	public MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}
}
