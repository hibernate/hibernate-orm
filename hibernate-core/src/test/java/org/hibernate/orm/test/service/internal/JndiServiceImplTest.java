/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.internal;

import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.jndi.internal.JndiServiceInitiator;
import org.hibernate.engine.jndi.spi.JndiService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks protocol restrictions being applied on the JNDI lookup capabilities
 * of the default JndiService implementation.
 */
public class JndiServiceImplTest {

	private final JndiService jndiService = JndiServiceInitiator.INSTANCE.initiateService( new HashMap<>(), null );

	@Test
	public void rejectNonLocalProtocols() {
		final JndiException ldapException = assertThrows( JndiException.class,
				() -> jndiService.locate(
						"ldap://yourserver/something" )
		);
		assertEquals( "JNDI lookups for scheme 'ldap' are not allowed", ldapException.getMessage() );
	}

	@Test
	public void javaLookupIsAttempted() {
		//The "java" scheme is allowed to be used; it will also fail as we didn't setup a full JNDI context
		//in this test, but we can verify it's been attempted by checking the error message.
		final JndiException javaLookupException = assertThrows( JndiException.class,
				() -> jndiService.locate(
						"java:comp/env/jdbc/MyDatasource" )
		);
		assertEquals( "Error parsing JNDI name [java:comp/env/jdbc/MyDatasource]", javaLookupException.getMessage() );
	}

}
