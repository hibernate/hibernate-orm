/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.UserType;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that a custom type which extends {@link org.hibernate.type.EnumType} continues to be
 * recognized as an EnumType rather than a basic custom type implementation since the values
 * which envers sends to describe the type in HBM differ whether its an Enum or not.
 *
 * Without the fix, this test would not even bootstrap and would throw a MappingException.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12304")
public class ExtendedEnumTypeTest extends BaseEnversJPAFunctionalTestCase {

	// An extended type to trigger the need for Envers to supply type information in the HBM mappings.
	// This should be treated the same as any other property annotated as Enumerated or uses an Enum.
	public static class ExtendedEnumType extends org.hibernate.type.EnumType {

	}

	@Entity(name = "Widget")
	@TypeDef(name = "extended_enum", typeClass = ExtendedEnumType.class)
	@Audited
	public static class Widget {
		@Id
		@GeneratedValue
		private Integer id;

		@Enumerated(EnumType.STRING)
		@Type(type = "extended_enum")
		private Status status;

		@Enumerated
		@Type(type = "extended_enum")
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Widget.class };
	}

	private Integer widgetId;

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1 - insert
		this.widgetId = doInJPA( this::entityManagerFactory, entityManager -> {
			final Widget widget = new Widget( Widget.Status.DRAFT );
			entityManager.persist( widget );
			return widget.getId();
		} );

		// Revision 2 - update
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Widget widget = entityManager.find( Widget.class, this.widgetId );
			widget.setStatus( Widget.Status.ARCHIVED );
			entityManager.merge( widget );
		} );

		// Revision 3 - delete
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Widget widget = entityManager.find( Widget.class, this.widgetId );
			entityManager.remove( widget );
		} );
	}

	@Test
	public void testRevisionHistory() {
		List revisions = getAuditReader().getRevisions( Widget.class, this.widgetId );
		assertEquals( Arrays.asList( 1, 2, 3 ), revisions );

		final Widget rev1 = getAuditReader().find( Widget.class, this.widgetId, 1 );
		assertEquals( Widget.Status.DRAFT, rev1.getStatus() );

		final Widget rev2 = getAuditReader().find( Widget.class, this.widgetId, 2 );
		assertEquals( Widget.Status.ARCHIVED, rev2.getStatus() );

		final Widget rev3 = getAuditReader().find( Widget.class, this.widgetId, 3 );
		assertNull( rev3 );
	}

	@Test
	public void testEnumPropertyStorageType() {
		// test that property 'status' translates to an enum type that is stored by name (e.g. STRING)
		assertEnumProperty( Widget.class, ExtendedEnumType.class, "status", EnumType.STRING );

		// test that property 'status2' translates to an enum type that is stored by position (e.g. ORDINAL)
		assertEnumProperty( Widget.class, ExtendedEnumType.class, "status2", EnumType.ORDINAL );
	}

	private void assertEnumProperty(Class<?> entityClass, Class<?> typeClass, String propertyName, EnumType expectedType) {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final SessionFactoryImplementor sessionFactory = entityManager.unwrap( SessionImplementor.class ).getSessionFactory();

			final EntityPersister entityPersister = sessionFactory.getMetamodel().entityPersister( entityClass );
			final EnversService enversService = sessionFactory.getServiceRegistry().getService( EnversService.class );

			final String entityName = entityPersister.getEntityName();
			final String auditEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );

			final EntityPersister auditedEntityPersister = sessionFactory.getMetamodel().entityPersister( auditEntityName );

			final org.hibernate.type.Type propertyType = auditedEntityPersister.getPropertyType( propertyName );
			assertTyping( CustomType.class, propertyType );

			final UserType userType = ( (CustomType) propertyType ).getUserType();
			assertTyping( typeClass, userType );
			assertTyping( org.hibernate.type.EnumType.class, userType );

			switch ( expectedType ) {
				case STRING:
					assertTrue( !( (org.hibernate.type.EnumType) userType ).isOrdinal() );
					break;
				default:
					assertTrue( ( (org.hibernate.type.EnumType) userType ).isOrdinal() );
					break;
			}
		} );
	}
}
