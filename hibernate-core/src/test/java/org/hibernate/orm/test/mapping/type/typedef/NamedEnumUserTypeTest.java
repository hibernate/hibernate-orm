/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.typedef;

import org.hibernate.orm.test.mapping.converted.enums.Gender;
import org.hibernate.orm.test.mapping.converted.enums.HairColor;
import org.hibernate.orm.test.mapping.converted.enums.Person;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the same UserType class used in multiple distinct TypeDefinitions
 * get distinct ManagedBeans.
 *
 * NOTE : this is with no local parameter
 *
 * @author gtoison
 */
@DomainModel(
		xmlMappings = "mappings/type/custom/typedef/PersonNamedEnumsUserType.xml"
)
@SessionFactory
public class NamedEnumUserTypeTest {
	@Test
	@JiraKey(value = "HHH-14820")
	public void testNamedEnumUserType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Person p", Person.class ).list();
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Person" ).executeUpdate();
		} );
	}
}
