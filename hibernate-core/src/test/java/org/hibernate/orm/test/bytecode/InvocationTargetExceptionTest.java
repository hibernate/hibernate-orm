/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/bytecode/Bean.xml")
@SessionFactory
public class InvocationTargetExceptionTest {
	@Test
	public void testProxiedInvocationException(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Bean bean = new Bean();
			bean.setSomeString( "my-bean" );
			s.persist( bean );
		} );

		factoryScope.inTransaction( (s) -> {
			Bean bean = s.getReference( Bean.class, "my-bean" );
			assertThat( Hibernate.isInitialized( bean ) ).isFalse();
			try {
				bean.throwException();
				fail( "exception not thrown" );
			}
			catch ( ParseException e ) {
				// expected behavior
			}
			catch ( Throwable t ) {
				fail( "unexpected exception type : " + t );
			}
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}
}
