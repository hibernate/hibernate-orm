/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		xmlMappings = "/org/hibernate/orm/test/mapping/converted/enums/Person.hbm.xml"
)
@SessionFactory
public class EnumExplicitTypeTest {

	@Test
	@JiraKey(value = "HHH-10766")
	public void hbmEnumWithExplicitTypeTest(SessionFactoryScope scope) {
		final Long id = scope.fromTransaction(
				(session) -> {
					Person person = Person.person( Gender.MALE, HairColor.BROWN );
					person.setOriginalHairColor( HairColor.BLONDE );
					session.persist( person );

					return person.getId();
				}
		);

		scope.inTransaction(
				(session) -> {
					Number personId = (Number) session.createNativeQuery(
							"select id from Person where originalHairColor = :color")
							.setParameter("color", HairColor.BLONDE.name())
							.getSingleResult();

					assertEquals( (long) id, personId.longValue() );
				}
		);
	}
}
