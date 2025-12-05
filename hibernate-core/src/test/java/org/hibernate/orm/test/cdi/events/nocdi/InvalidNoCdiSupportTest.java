/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events.nocdi;

import org.hibernate.InstantiationException;
import org.hibernate.orm.test.cdi.events.Monitor;
import org.hibernate.orm.test.cdi.events.TheEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Attempt to use CDI injection when no CDI is available
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ExtendWith( Monitor.Resetter.class )
@ServiceRegistry
@DomainModel(annotatedClasses = TheEntity.class)
public class InvalidNoCdiSupportTest {
	@Test
	public void testIt(DomainModelScope modelScope) {
		// because there is no CDI available, building the SF should immediately
		// try to build the ManagedBeans which should fail here
		try (var sf = modelScope.getDomainModel().buildSessionFactory()) {
			Assertions.fail( "Expecting failure" );
		}
		catch (InstantiationException expected) {
		}
	}
}
