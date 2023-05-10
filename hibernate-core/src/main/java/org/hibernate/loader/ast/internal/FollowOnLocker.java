/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.AfterLoadAction;
import org.hibernate.metamodel.mapping.EntityMappingType;

import jakarta.persistence.PessimisticLockScope;

/**
 * AfterLoadAction implementation for applying follow-on locking
 *
 * @author Steve Ebersole
 */
public class FollowOnLocker implements AfterLoadAction {
	private final LockOptions lockOptions;

	public FollowOnLocker(LockMode lockMode, int timeOut, PessimisticLockScope lockScope) {
		lockOptions = new LockOptions( lockMode );
		lockOptions.setTimeOut( timeOut );
		lockOptions.setLockScope( lockScope );
	}

	@Override
	public void afterLoad(
			Object entity,
			EntityMappingType entityMappingType,
			SharedSessionContractImplementor session) {
		session.asSessionImplementor().lock( entityMappingType.getEntityName(), entity, lockOptions );
	}
}
