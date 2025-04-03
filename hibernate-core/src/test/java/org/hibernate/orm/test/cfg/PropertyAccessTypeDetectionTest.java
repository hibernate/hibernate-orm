/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value ="HHH-12199")
@DomainModel(xmlMappings = "org/hibernate/orm/test/cfg/FooEntity.xml")
@SessionFactory
public class PropertyAccessTypeDetectionTest {
	public static class FooEntity {
		public static final String intValue = "intValue";

		private Long id;
		private Integer _intValue;

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }

		public Integer getIntValue() { return _intValue; }
		public void setIntValue(Integer intValue) { this._intValue = intValue; }
	}

	@Test
	public void testPropertyAccessIgnoresStaticFields(SessionFactoryScope factoryScope) {
		// verify that the entity persister is configured with property intValue as an Integer rather than
		// using the static field reference and determining the type to be String.
		final EntityPersister entityDescriptor = factoryScope
				.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( FooEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "intValue" );
		assertThat( attributeMapping ).isNotNull();
		assertThat( attributeMapping.getJavaType().getJavaTypeClass() ).isAssignableFrom( Integer.class );
	}
}
