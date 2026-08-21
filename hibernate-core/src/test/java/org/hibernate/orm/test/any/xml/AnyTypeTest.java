/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-1663")
@ServiceRegistry( settings = @Setting( name = USE_SECOND_LEVEL_CACHE, value = "false" ) )
@DomainModel( xmlMappings = "org/hibernate/orm/test/any/xml/Person.xml")
@SessionFactory
public class AnyTypeTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		final Person person = new Person();
		final Address address = new Address();
		person.setData( address );

		scope.inTransaction(
				session -> {
					session.persist( person );
					session.persist( address );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testStoredData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Person person = session.createQuery( "from Person", Person.class ).uniqueResult();
					assertThat( person.getData(), instanceOf( Address.class ) );
				}
		);
	}
}
