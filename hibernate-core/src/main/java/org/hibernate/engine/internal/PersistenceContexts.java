/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.SessionImpl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Operations to instantiate and (de)serialize {@link StatefulPersistenceContext}
 * without exposing the class outside this package.
 *
 * @author Gavin King
 */
public class PersistenceContexts {
	public static PersistenceContext createPersistenceContext(SharedSessionContractImplementor session) {
		return new StatefulPersistenceContext( session );
	}

	public static PersistenceContext deserialize(ObjectInputStream ois, SessionImpl session)
			throws IOException, ClassNotFoundException {
		return StatefulPersistenceContext.deserialize( ois, session );
	}

	public static void serialize(PersistenceContext persistenceContext, ObjectOutputStream oos)
			throws IOException {
		( (StatefulPersistenceContext) persistenceContext ).serialize( oos );
	}
}
