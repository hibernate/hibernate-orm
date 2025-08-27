/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.basic;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * <pre>
 * Javadoc to @{@link Basic} :
 * The use of the Basic annotation is optional for persistent fields and properties of these types.
 * If the Basic annotation is not specified for such a field or property, the default values of the Basic annotation apply.
 *
 * And the property :
 *
 * <code>boolean optional() default true;</code>
 *
 * But if we add the @Basic annotation or not on a boolean type, we do not get the same result.
 * In one case, the boolean is optional, while in the other, it is not, which contradicts the Javadoc.
 * </pre>
 *
 * @author Vincent Bouthinon
 */
@ServiceRegistry()
@JiraKey("HHH-19279")
class ImplicitOptionalBooleanBasicTest {

	@Test
	void testImplicitOptionalBooleanBasic(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( BooleanBasicTest.class );
		Metadata metadata = metadataSources.buildMetadata();
		PersistentClass entityBinding = metadata.getEntityBinding( BooleanBasicTest.class.getName() );
		assertFalse( entityBinding.getProperty( "booleanWithBasic" ).isOptional(), "primitive property is optional" );
		assertFalse( entityBinding.getProperty( "booleanWithoutBasic" ).isOptional(), "primitive property is optional" );
	}

	@Entity
	public static class BooleanBasicTest {

		@Id
		@GeneratedValue
		private Long id;

		@Basic
		private boolean booleanWithBasic;

		private boolean booleanWithoutBasic;
	}
}
