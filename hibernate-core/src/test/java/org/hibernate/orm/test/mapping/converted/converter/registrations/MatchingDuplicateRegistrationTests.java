/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import org.hibernate.annotations.ConverterRegistration;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
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
@DomainModel( annotatedClasses = MatchingDuplicateRegistrationTests.DupRegEntity.class )
public class MatchingDuplicateRegistrationTests {
	@Test
	public void verifyMapping(DomainModelScope domainModelScope) {
		// just getting here indicates success
		//		- although asserting that the log message happens would be nice
	}

	@Entity( name = "DupRegEntity" )
	@Table( name = "DupRegEntity" )
	@ConverterRegistration( converter = Thing1Converter.class )
	@ConverterRegistration( converter = Thing1Converter.class )
	public static class DupRegEntity {
		@Id
		private Integer id;
		private String name;
		private Thing1 thing1;

		private DupRegEntity() {
			// for use by Hibernate
		}

		public DupRegEntity(Integer id, String name) {
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
