/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.n_ative.local;


import org.hibernate.generator.Generator;
import org.hibernate.id.NativeGenerator;
import org.hibernate.mapping.KeyValue;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = NativeGeneratorClassTest.NativeEntity.class)
public class NativeGeneratorClassTest {
	@Test void test(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		scope.inTransaction(s -> s.persist(new NativeEntity()));

		final KeyValue identifier = domainModelScope.getEntityBinding( NativeEntity.class ).getIdentifier();
		final Generator generator = identifier.createGenerator( null, null, null );
		assertThat( generator ).isInstanceOf( NativeGenerator.class );
	}

	@Entity
	@org.hibernate.annotations.NativeGenerator
	public static class NativeEntity {
		@Id @GeneratedValue
		long id;
		String data;
	}
}
