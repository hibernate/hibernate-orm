/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.enumcollection;

import java.util.List;
import java.util.Set;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mote that these are simply performing syntax checking (can the criteria query
 * be properly compiled and executed)
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {User.class})
public class EnumIsMemberTest {

	@Test
	@JiraKey(value = "HHH-9605")
	public void testQueryEnumCollection(EntityManagerFactoryScope scope) {
		final User user = new User();
		scope.inTransaction( entityManager -> {
			user.setId( 1L );
			user.getRoles().add( User.Role.Admin );
			entityManager.persist( user );
		} );

		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<User> query = builder.createQuery( User.class );
			Root<User> root = query.from( User.class );
			Expression<Set<User.Role>> roles = root.get( User_.roles );
			// Using the correct collection of enums and an enum parameter
			query.where( builder.isMember( User.Role.Admin, roles ) );
			TypedQuery<User> typedQuery = entityManager.createQuery( query );

			List<User> users = typedQuery.getResultList();
			assertEquals( 1, users.size() );
		} );

		scope.inTransaction( entityManager -> {
			// delete
			entityManager.remove( entityManager.find(User.class, 1L ) );
		} );
	}

}
