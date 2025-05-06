/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Jpa(
		annotatedClasses = {
				TestEntity.class,
				Person.class
		}
)
@JiraKey(value = "HHH-15285")
public class MetamodelJavaTypeTest {

	@Test
	public void testJavaType(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					assertThat( TestEntity_.people.getJavaType() ).isEqualTo( List.class );

					assertThat( TestEntity_.addresses.getJavaType() ).isEqualTo( List.class );

					assertThat( TestEntity_.elementCollection.getJavaType() ).isEqualTo( List.class );
				}
		);
	}
}
