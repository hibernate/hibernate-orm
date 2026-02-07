/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = UpsertNaturalIdTest.Natural.class)
class UpsertNaturalIdTest {
	@Test
	void test(SessionFactoryScope scope) {
		var natural = new Natural();
		natural.id = 69;
		natural.code = "000";
		natural.text = "Lorem ipsum";
		scope.inStatelessTransaction( s -> {
			s.upsert( natural );
		} );
		scope.inStatelessTransaction( s -> {
			assertEquals("000", s.get( Natural.class, 69 ).code);
		} );
		scope.inStatelessTransaction( s -> {
			s.upsert( natural );
		} );
		scope.inStatelessTransaction( s -> {
			assertEquals("000", s.get( Natural.class, 69 ).code);
		} );
	}
	@Entity
	static class Natural {
		@Id long id;
		@NaturalId String code;
		String text;
	}
}
