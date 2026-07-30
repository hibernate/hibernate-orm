/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.n_ative.local;

import java.util.UUID;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.NativeGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.orm.test.idgen.GeneratorSettingsImpl;
import org.hibernate.testing.util.uuid.IdGeneratorCreationContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(
		name = AvailableSettings.DIALECT,
		value = "org.hibernate.orm.test.idgen.n_ative.local.NativeGeneratorUuidDialectTest$UuidNativeDialect"
))
@SessionFactory
@DomainModel(annotatedClasses = NativeGeneratorUuidDialectTest.NativeEntity.class)
public class NativeGeneratorUuidDialectTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void test(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new NativeEntity() ) );

		final PersistentClass entityBinding = domainModelScope.getEntityBinding( NativeEntity.class );
		final Property idProperty = entityBinding.getIdentifierProperty();
		final SimpleValue identifier = (SimpleValue) entityBinding.getIdentifier();
		final var creationContext =
				new IdGeneratorCreationContext( domainModelScope.getDomainModel(), entityBinding.getRootClass() );

		assertThat( identifier.getCustomIdGeneratorCreator().getGeneratorClass( creationContext ) )
				.isEqualTo( org.hibernate.id.uuid.UuidGenerator.class );

		final Generator generator = GeneratorSettingsImpl.createIdentifierGenerator(
				identifier,
				domainModelScope.getDomainModel().getDatabase().getDialect(),
				entityBinding.getRootClass(),
				idProperty,
				domainModelScope.getDomainModel()
		);
		assertThat( generator ).isInstanceOf( NativeGenerator.class );
	}

	@Entity(name = "NativeUuidEntity")
	public static class NativeEntity {
		@Id
		@GeneratedValue
		@org.hibernate.annotations.NativeGenerator
		UUID id;
	}

	public static class UuidNativeDialect extends H2Dialect {
		@Override
		public GenerationType getNativeValueGenerationStrategy() {
			return GenerationType.UUID;
		}
	}
}
