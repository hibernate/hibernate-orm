/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events.nocdi;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests valid use of ManagedBeanRegistry when CDI is not available -
 * meaning injection is not requested.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ExtendWith( ValidNoCdiSupportTest.AnotherListener.Resetter.class )
@ServiceRegistry
@DomainModel(annotatedClasses = ValidNoCdiSupportTest.AnotherEntity.class)
@SessionFactory
public class ValidNoCdiSupportTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	public void testIt(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();

		// The CDI bean should have been built immediately...
		assertTrue( AnotherListener.wasInstantiated() );
		assertEquals( 0, AnotherListener.currentCount() );

		factoryScope.inTransaction( (session) -> {
			session.persist( new AnotherEntity( 1 ) );
		} );

		assertEquals( 1, AnotherListener.currentCount() );
	}

	@Entity( name = "AnotherEntity" )
	@Table( name = "another_entity")
	@EntityListeners( AnotherListener.class )
	public static class AnotherEntity {
		private Integer id;
		private String name;

		public AnotherEntity() {
		}

		public AnotherEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class AnotherListener {
		private static final AtomicInteger count = new AtomicInteger( 0 );
		private static boolean instantiated;

		public AnotherListener() {
			instantiated = true;
		}

		public static void reset() {
			count.set( 0 );
			instantiated = false;
		}

		public static boolean wasInstantiated() {
			return instantiated;
		}

		public static int currentCount() {
			return count.get();
		}

		@PrePersist
		public void onCreate(Object entity) {
			count.getAndIncrement();
		}

		public static class Resetter implements BeforeEachCallback {
			@Override
			public void beforeEach(ExtensionContext context) throws Exception {
				AnotherListener.reset();
			}
		}
	}
}
