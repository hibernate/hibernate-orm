/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.nativequery;

import java.util.List;

import jakarta.persistence.TypedQuery;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12776")
@EnversTest
@Jpa(annotatedClasses = {SimpleEntity.class, SecondSimpleEntity.class})
public class EntityResultNativeQueryTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.persist( new SimpleEntity( "Hibernate" )) );
	}

	@Test
	public void testNativeQueryResultHandling(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			TypedQuery<SimpleEntity> query = em.createNativeQuery( "select * from SimpleEntity", SimpleEntity.class );
			List<SimpleEntity> results = query.getResultList();
			SimpleEntity result = results.get( 0 );
			assertEquals( "Hibernate", result.getStringField() );
		} );
	}
}
