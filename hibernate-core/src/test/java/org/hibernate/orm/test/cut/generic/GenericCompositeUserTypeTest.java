/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut.generic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


@JiraKey(value = "HHH-17019")
@DomainModel(
		annotatedClasses = {
				GenericCompositeUserTypeEntity.class
		}
)
@SessionFactory
public class GenericCompositeUserTypeTest {

	@Test
	public void hhh17019Test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EnumPlaceholder<Weekdays, Weekdays> placeholder = new EnumPlaceholder<>( Weekdays.MONDAY, Weekdays.SUNDAY );
			GenericCompositeUserTypeEntity entity = new GenericCompositeUserTypeEntity( placeholder );

			session.persist( entity );
		} );
	}
}
