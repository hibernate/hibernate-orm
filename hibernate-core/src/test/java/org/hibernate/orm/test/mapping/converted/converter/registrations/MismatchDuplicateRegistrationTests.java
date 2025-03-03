/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tests multiple registrations of conversions for a particular domain-type
 * where the definitions match - should simply skip the subsequent registrations
 *
 * @author Steve Ebersole
 */
@ServiceRegistry
public class MismatchDuplicateRegistrationTests {
	@Test
	@ExpectedException(AnnotationException.class)
	public void verifyMappingError(ServiceRegistryScope registryScope) {
		new MetadataSources( registryScope.getRegistry() ).addAnnotatedClass( TroublesomeEntity.class ).buildMetadata();
	}

	@Entity( name = "TroublesomeEntity" )
	@Table( name = "TroublesomeEntity" )
	@ConverterRegistration( converter = Thing1Converter.class )
	@ConverterRegistration( converter = Thing1Converter.class, autoApply = false )
	public static class TroublesomeEntity {
		@Id
		private Integer id;
		private String name;
		private Thing1 thing1;

		private TroublesomeEntity() {
			// for use by Hibernate
		}

		public TroublesomeEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Thing1 getThing1() {
			return thing1;
		}

		public void setThing1(Thing1 thing1) {
			this.thing1 = thing1;
		}
	}
}
