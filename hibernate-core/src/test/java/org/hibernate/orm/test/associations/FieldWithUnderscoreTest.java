/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = {FieldWithUnderscoreTest.A.class, FieldWithUnderscoreTest.B.class})
public class FieldWithUnderscoreTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inSession(s -> s.createSelectionQuery("from B join _a", B.class).getResultList());
		scope.inSession(s -> s.createSelectionQuery("from B left join fetch _a", B.class).getResultList());
	}

	@Entity(name = "A")
	static class A {
		@Id
		@Column(name = "id")
		Long _id;

	}
	@Entity(name = "B")
	static class B {
		@Id
		@Column(name = "id")
		Long _id;
		@JoinColumn(name = "relation_a")
		@ManyToOne(fetch = FetchType.LAZY) A _a;
	}
}
