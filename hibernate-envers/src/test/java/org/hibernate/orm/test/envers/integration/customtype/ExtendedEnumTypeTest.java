/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Type;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that a custom type which extends {@link org.hibernate.orm.test.envers.integration.customtype.EnumType}
 * continues to be recognized as an EnumType rather than a basic custom type implementation since the values
 * which envers sends to describe the type in HBM differ whether its an Enum or not.
 *
 * Without the fix, this test would not even bootstrap and would throw a MappingException.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12304")
@EnversTest
@Jpa(annotatedClasses = ExtendedEnumTypeTest.Widget.class)
public class ExtendedEnumTypeTest {

	// An extended type to trigger the need for Envers to supply type information in the HBM mappings.
	// This should be treated the same as any other property annotated as Enumerated or uses an Enum.
	public static class ExtendedEnumType extends org.hibernate.orm.test.envers.integration.customtype.EnumType<Widget.Status> {

	}

	@Entity(name = "Widget")
	@Audited
	public static class Widget {
		@Id
		@GeneratedValue
		private Integer id;

		@Type( ExtendedEnumType.class )
		@Enumerated(EnumType.STRING)
		private Status status;

		@Enumerated
		@Type( ExtendedEnumType.class )
		private Status status2;

		public enum Status {
			ARCHIVED,
			DRAFT
		}

		Widget() {

		}

		Widget(Status status) {
			this.status = status;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public Status getStatus2() {
			return status2;
		}

		public void setStatus2(Status status2) {
			this.status2 = status2;
		}
	}

	private Integer widgetId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - insert
		this.widgetId = scope.fromTransaction( entityManager -> {
			final Widget widget = new Widget( Widget.Status.DRAFT );
			entityManager.persist( widget );
			return widget.getId();
		} );

		// Revision 2 - update
		scope.inTransaction( entityManager -> {
			final Widget widget = entityManager.find( Widget.class, this.widgetId );
			widget.setStatus( Widget.Status.ARCHIVED );
			entityManager.merge( widget );
		} );

		// Revision 3 - delete
		scope.inTransaction( entityManager -> {
			final Widget widget = entityManager.find( Widget.class, this.widgetId );
			entityManager.remove( widget );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List revisions = auditReader.getRevisions( Widget.class, this.widgetId );
			assertEquals( Arrays.asList( 1, 2, 3 ), revisions );

			final Widget rev1 = auditReader.find( Widget.class, this.widgetId, 1 );
			assertEquals( Widget.Status.DRAFT, rev1.getStatus() );

			final Widget rev2 = auditReader.find( Widget.class, this.widgetId, 2 );
			assertEquals( Widget.Status.ARCHIVED, rev2.getStatus() );

			final Widget rev3 = auditReader.find( Widget.class, this.widgetId, 3 );
			assertNull( rev3 );
		} );
	}

	@Test
	public void testEnumPropertyStorageType(EntityManagerFactoryScope scope) {
		// test that property 'status' translates to an enum type that is stored by name (e.g. STRING)
		assertEnumProperty( scope, Widget.class, ExtendedEnumType.class, "status", EnumType.STRING );

		// test that property 'status2' translates to an enum type that is stored by position (e.g. ORDINAL)
		assertEnumProperty( scope, Widget.class, ExtendedEnumType.class, "status2", EnumType.ORDINAL );
	}

	private void assertEnumProperty(EntityManagerFactoryScope scope, Class<?> entityClass, Class<?> typeClass, String propertyName, EnumType expectedType) {
		scope.inEntityManager( entityManager -> {
			final SessionFactoryImplementor sessionFactory = entityManager.unwrap( SessionImplementor.class ).getSessionFactory();

			final EntityPersister entityPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityClass );
			final EnversService enversService = sessionFactory.getServiceRegistry().getService( EnversService.class );

			final String entityName = entityPersister.getEntityName();
			final String auditEntityName = enversService.getConfig().getAuditEntityName( entityName );

			final EntityPersister auditedEntityPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( auditEntityName );

			final org.hibernate.type.Type propertyType = auditedEntityPersister.getPropertyType( propertyName );
			assertTyping( CustomType.class, propertyType );

			final UserType userType = ( (CustomType<Object>) propertyType ).getUserType();
			assertTyping( typeClass, userType );
			assertTyping( org.hibernate.orm.test.envers.integration.customtype.EnumType.class, userType );

			// org,hibernate.type.EnumType used to be special-cased in the Envers code
//			switch ( expectedType ) {
//				case STRING:
//					assertTrue( !( (org.hibernate.envers.test.integration.customtype.EnumType) userType ).isOrdinal() );
//					break;
//				default:
//					assertTrue( ( (org.hibernate.envers.test.integration.customtype.EnumType) userType ).isOrdinal() );
//					break;
//			}
		} );
	}
}
