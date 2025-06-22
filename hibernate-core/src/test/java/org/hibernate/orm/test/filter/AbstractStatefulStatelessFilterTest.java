/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.params.provider.Arguments;

@SessionFactory
public abstract class AbstractStatefulStatelessFilterTest implements SessionFactoryScopeAware {

	protected SessionFactoryScope scope;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	protected List<? extends Arguments> transactionKind() {
		// We want to test both regular and stateless session:
		BiConsumer<SessionFactoryScope, Consumer<SessionImplementor>> kind1 = SessionFactoryScope::inTransaction;
		BiConsumer<SessionFactoryScope, Consumer<StatelessSession>> kind2 = SessionFactoryScope::inStatelessTransaction;
		return List.of(
				Arguments.of( kind1 ),
				Arguments.of( kind2 )
		);
	}
}
