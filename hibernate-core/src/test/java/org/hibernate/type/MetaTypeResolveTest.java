/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MetaType#resolveDiscriminatorValue(EntityPersister)} and {@link MetaType#resolveEntityName(String)}
 *
 * @author Vincent Bouthinon
 */
@DomainModel(
		annotatedClasses = {
				MetaTypeResolveTest.Dog.class
		}
)
@SessionFactory
@JiraKey("HHH-18684")
class MetaTypeResolveTest {

	@Test
	void resolveDiscriminatorValue_must_return_the_discriminator_value_when_the_entity_name_exist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Given
					EntityPersister dogPersister = session.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( MetaTypeResolveTest.Dog.class );
					Map<Object, String> discriminatorValuesToEntityNameMap = new HashMap<>();
					discriminatorValuesToEntityNameMap.put( "dog", dogPersister.getEntityName() );
					MetaType metaType = new MetaType( discriminatorValuesToEntityNameMap, null );
					// When
					Object discriminatorValue = metaType.resolveDiscriminatorValue( dogPersister );
					// Then
					assertThat( discriminatorValue ).isEqualTo( "dog" );
				} );
	}

	@Test
	void resolveDiscriminatorValue_must_return_the_entity_name_when_the_entity_name_dont_exist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Given
					EntityPersister dogPersister = session.getSessionFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( MetaTypeResolveTest.Dog.class );
					Map<Object, String> discriminatorValuesToEntityNameMap = new HashMap<>();
					discriminatorValuesToEntityNameMap.put( "anotherClass",
							MetaTypeResolveTest.class.getCanonicalName() );
					MetaType metaType = new MetaType( discriminatorValuesToEntityNameMap, null );
					// When
					Object discriminatorValue = metaType.resolveDiscriminatorValue( dogPersister );
					// Then
					assertThat( discriminatorValue ).isEqualTo( dogPersister.getEntityName() );
				} );
	}

	@Test
	void resolveEntityName_must_return_the_entity_name_when_the_discriminator_value_exist() {
		// Given
		Map<Object, String> discriminatorValuesToEntityNameMap = new HashMap<>();
		discriminatorValuesToEntityNameMap.put( "dog", Dog.class.getCanonicalName() );
		MetaType metaType = new MetaType( discriminatorValuesToEntityNameMap, null );
		// When
		Object resolveEntityName = metaType.resolveEntityName( "dog" );
		// Then
		assertThat( resolveEntityName ).isEqualTo( Dog.class.getCanonicalName() );
	}

	@Test
	void resolveEntityName_must_return_discriminator_value_when_the_entity_name_dont_exist() {
		// Given
		Map<Object, String> discriminatorValuesToEntityNameMap = new HashMap<>();
		MetaType metaType = new MetaType( discriminatorValuesToEntityNameMap, null );
		// When
		Object resolveEntityName = metaType.resolveEntityName( Dog.class.getCanonicalName() );
		// Then
		assertThat( resolveEntityName ).isEqualTo( Dog.class.getCanonicalName() );
	}

	@Entity
	public static class Dog {
		@Id
		public int id;
	}
}
