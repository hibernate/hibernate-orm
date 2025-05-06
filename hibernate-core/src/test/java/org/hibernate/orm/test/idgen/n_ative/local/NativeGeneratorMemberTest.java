/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.n_ative.local;


import org.hibernate.generator.Generator;
import org.hibernate.id.NativeGenerator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.idgen.n_ative.GeneratorSettingsImpl;

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
@DomainModel(annotatedClasses = NativeGeneratorMemberTest.NativeEntity.class)
public class NativeGeneratorMemberTest {
	@Test void test(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		scope.inTransaction(s -> s.persist(new NativeEntity()));

		final PersistentClass entityBinding = domainModelScope.getEntityBinding( NativeEntity.class );
		final Property idProperty = entityBinding.getIdentifierProperty();
		final KeyValue identifier = entityBinding.getIdentifier();

		final Generator generator = identifier.createGenerator(
				domainModelScope.getDomainModel().getDatabase().getDialect(),
				entityBinding.getRootClass(),
				idProperty,
				new GeneratorSettingsImpl( domainModelScope.getDomainModel() )
		);
		assertThat( generator ).isInstanceOf( NativeGenerator.class );
	}

	@Entity
	public static class NativeEntity {
		@Id @GeneratedValue
		@org.hibernate.annotations.NativeGenerator
		long id;
		String data;
	}
}
