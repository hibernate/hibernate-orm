/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import org.hibernate.MappingException;
import org.hibernate.internal.util.ExceptionHelper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for mapping wrapper values
 */
@DomainModel(annotatedClasses = WrapperArrayHandlingDisallowTests.EntityOfByteArrays.class)
@SessionFactory
public class WrapperArrayHandlingDisallowTests {

	@Test
	public void verifyByteArrayMappings(SessionFactoryScope scope) {
		try {
			scope.getSessionFactory();
			Assertions.fail( "Should fail boot validation!" );
		}
		catch (Exception e) {
			final Throwable rootCause = ExceptionHelper.getRootCause( e );
			assertEquals( MappingException.class, rootCause.getClass() );
			assertThat( rootCause.getMessage(), containsString( WrapperArrayHandlingDisallowTests.EntityOfByteArrays.class.getName() + ".wrapper" ) );
		}
	}

	@Entity(name = "EntityOfByteArrays")
	@Table(name = "EntityOfByteArrays")
	public static class EntityOfByteArrays {
		@Id
		public Integer id;
		private Byte[] wrapper;

		public EntityOfByteArrays() {
		}
	}
}
