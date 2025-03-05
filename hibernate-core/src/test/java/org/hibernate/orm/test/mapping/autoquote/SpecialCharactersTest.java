/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.autoquote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses = SpecialCharactersTest.Simple.class)
public class SpecialCharactersTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory();
	}
	@Entity static class Simple {
		@Id
		long id;
		@Column(name="NAME$NAME")
		String nameWithDollar;
		@Column(name="$NAME")
		String nameWithInitialDollar;
		@Column(name="NAME#NAME")
		String nameWithHash;
		@Column(name="NAME NAME")
		String nameWithSpace;
		@Column(name="NAME_NAME")
		String nameWithUnderscore;
		@Column(name="_NAME")
		String nameWithInitialUnderscore;
	}
}
