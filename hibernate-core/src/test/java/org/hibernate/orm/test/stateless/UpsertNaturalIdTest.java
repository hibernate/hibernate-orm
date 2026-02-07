/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = UpsertNaturalIdTest.Natural.class)
class UpsertNaturalIdTest {
	@Test
	void test(SessionFactoryScope scope) {
		var natural = new Natural();
		natural.id = 69;
		natural.code = "000";
		natural.text = "Lorem ipsum";
		natural.uuid = UUID.randomUUID();
		scope.inStatelessTransaction( s -> {
			s.upsert( natural );
		} );
		scope.inStatelessTransaction( s -> {
			Natural nat = s.get( Natural.class, 69 );
			assertEquals("000", nat.code);
			assertEquals( "Lorem ipsum", nat.text );
			assertNotNull( nat.uuid );
		} );
		scope.inStatelessTransaction( s -> {
			natural.uuid = null;
			natural.text = "Lorem ipsum dolor sit amet";
			s.upsert( natural );
		} );
		scope.inStatelessTransaction( s -> {
			Natural nat = s.get( Natural.class, 69 );
			assertEquals("000", nat.code);
			assertEquals( "Lorem ipsum dolor sit amet", nat.text );
			assertNotNull( nat.uuid );
		} );
	}
	@Entity
	static class Natural {
		@Id long id;
		@NaturalId String code;
		String text;
		@Column(updatable = false)
		UUID uuid;
	}
}
