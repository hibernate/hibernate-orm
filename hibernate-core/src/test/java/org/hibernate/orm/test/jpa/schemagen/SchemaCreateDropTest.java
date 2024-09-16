/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import jakarta.persistence.TypedQuery;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10104")
@Jpa(annotatedClasses = {
		Document.class
})
public class SchemaCreateDropTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Document( "hibernate" ) );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Document" ).executeUpdate();
				}
		);
	}

	@Test
	public void testQueryWithoutTransaction(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					TypedQuery<String> query = entityManager.createQuery( "SELECT d.name FROM Document d", String.class );
					List<String> results = query.getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}
}
