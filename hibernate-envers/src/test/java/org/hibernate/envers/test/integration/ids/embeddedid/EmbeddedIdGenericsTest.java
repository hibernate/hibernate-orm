/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * Tests an entity mapping that uses an {@link EmbeddedId} mapping that makes use of generics.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-13564")
public class EmbeddedIdGenericsTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NotificationType.class, Trigger.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		// Store NotificationType and Trigger instance
		doInJPA( this::entityManagerFactory, entityManager -> {
			final NotificationType type = new NotificationType( "code" );
			entityManager.persist( type );

			Trigger trigger = new Trigger( "str", type );
			entityManager.persist( trigger );

			trigger.setActive( !trigger.isActive() );
			entityManager.merge( trigger );
		} );
	}

	@Test
	public void testAuditQueryMappedSuperclassWithEmbeddedId() {
		// There should be at least one revision for Trigger
		List resultList = getAuditReader().createQuery().forRevisionsOfEntity( Trigger.class, true, true ).getResultList();
		assertEquals( 1, resultList.size() );

		// Trigger should be hydrated with a composite-id values below
		Trigger entityInstance = (Trigger) resultList.get( 0 );
		assertEquals( "str", entityInstance.getPk().getEventType() );
		assertEquals( "code", entityInstance.getPk().getNotificationType().getCode() );
	}


	@MappedSuperclass
	public abstract static class CompositeIdBaseEntity<PK extends Serializable> implements Serializable {
		protected PK pk;

		@EmbeddedId
		public PK getPk() {
			return pk;
		}

		public void setPk(PK pk) {
			this.pk = pk;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CompositeIdBaseEntity<?> that = (CompositeIdBaseEntity<?>) o;
			return Objects.equals( pk, that.pk );
		}

		@Override
		public int hashCode() {
			return Objects.hash( pk );
		}
	}

	@Audited
	@Entity(name = "Trigger")
	@Table(name = "`Trigger`")
	public static class Trigger extends CompositeIdBaseEntity<Trigger.TriggerPK> {
		private boolean active;

		Trigger() {

		}

		public Trigger(String eventType, NotificationType notificationType) {
			this.pk = new TriggerPK( eventType, notificationType );
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		@Embeddable
		public static class TriggerPK implements Serializable {
			private String eventType;
			private NotificationType notificationType;

			TriggerPK() {

			}

			public TriggerPK(String eventType, NotificationType notificationType) {
				this.eventType = eventType;
				this.notificationType = notificationType;
			}

			@Column(nullable = false, insertable = false, updatable = false)
			public String getEventType() {
				return eventType;
			}

			public void setEventType(String eventType) {
				this.eventType = eventType;
			}

			@Audited(targetAuditMode =  RelationTargetAuditMode.NOT_AUDITED)
			@ManyToOne
			@JoinColumn(insertable = false, updatable = false, nullable = false)
			public NotificationType getNotificationType() {
				return notificationType;
			}

			public void setNotificationType(NotificationType notificationType) {
				this.notificationType = notificationType;
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				TriggerPK triggerPK = (TriggerPK) o;
				return Objects.equals( eventType, triggerPK.eventType ) &&
						Objects.equals( notificationType, triggerPK.notificationType );
			}

			@Override
			public int hashCode() {
				return Objects.hash( eventType, notificationType );
			}
		}
	}

	@Entity(name = "NotificationType")
	public static class NotificationType {
		@Id
		private String code;

		public NotificationType() {

		}

		public NotificationType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			NotificationType that = (NotificationType) o;
			return Objects.equals( code, that.code );
		}

		@Override
		public int hashCode() {
			return Objects.hash( code );
		}
	}
}
