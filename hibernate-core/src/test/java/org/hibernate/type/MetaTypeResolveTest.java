/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MetaType#resolveDiscriminatorValue(String)} and {@link MetaType#resolveEntityName(String)}
 *
 * @author Vincent Bouthinon
 */
@JiraKey("HHH-18684")
class MetaTypeResolveTest {

	@Test
	void resolveDiscriminatorValue_must_return_the_discriminator_value_when_the_entity_name_exist() {
		// Given
		Map<Object, String> discriminatorValuesToEntityNameMap = new HashMap<>();
		discriminatorValuesToEntityNameMap.put( "dog", Dog.class.getCanonicalName() );
		MetaType metaType = new MetaType( discriminatorValuesToEntityNameMap, null );
		// When
		Object discriminatorValue = metaType.resolveDiscriminatorValue( Dog.class.getCanonicalName() );
		// Then
		assertThat( discriminatorValue ).isEqualTo( "dog" );
	}

	@Test
	void resolveDiscriminatorValue_must_return_the_entity_name_when_the_entity_name_dont_exist() {
		// Given
		Map<Object, String> discriminatorValuesToEntityNameMap = new HashMap<>();
		discriminatorValuesToEntityNameMap.put( "anotherClass", MetaTypeResolveTest.class.getCanonicalName() );
		MetaType metaType = new MetaType( discriminatorValuesToEntityNameMap, null );
		// When
		Object discriminatorValue = metaType.resolveDiscriminatorValue( Dog.class.getCanonicalName() );
		// Then
		assertThat( discriminatorValue ).isEqualTo( Dog.class.getCanonicalName() );
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

	private static class Dog {
	}
}