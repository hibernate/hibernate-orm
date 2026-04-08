/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


@DomainModel(annotatedClasses = {
		DynamicUpdateEmbeddableInEntityListenerTest.JpaEntity.class,
})
@SessionFactory
public class DynamicUpdateEmbeddableInEntityListenerTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-9754")
	public void testEntityListenerUpdateEmbeddable(SessionFactoryScope scope) {
		JpaEntity persistedEntity = scope.fromTransaction( session -> {
			JpaEntity entity = new JpaEntity();
			entity.setName( "Initial name" );
			session.persist( entity );
			return entity;
		} );
		scope.inTransaction( session -> {
			JpaEntity toMerge = new JpaEntity();
			toMerge.setId( persistedEntity.getId() );
			toMerge.setName( "Updated" );
			toMerge.setCreatedOn( persistedEntity.getCreatedOn() );
			toMerge.setLastModifiedOn( persistedEntity.getLastModifiedOn() );
			session.merge( toMerge );
		} );

		JpaEntity finalVersion =
				scope.fromTransaction( session -> session.find( JpaEntity.class, persistedEntity.getId() ) );
		assertEquals( "Updated", finalVersion.getName() );

		// Ensure that the update operation considered changes done to embeddable through entity listener
		assertNotEquals( finalVersion.getCreatedOn().getTime(), finalVersion.getLastModifiedOn().getTime() );
	}

	@Entity(name = "JpaEntity")
	@DynamicInsert
	@DynamicUpdate
	@Table(name = "entity")
	public static class JpaEntity {
		@Id
		@GeneratedValue
		private Long id;
		public String name;
		@Embedded
		private AuditSupportColumns auditSupportColumns = new AuditSupportColumns();

		@PrePersist
		public void onCreate() {
			if ( getAuditSupportColumns() == null ) {
				setAuditSupportColumns( new AuditSupportColumns() );
			}
			getAuditSupportColumns().setCreatedOn( new Date() );
			getAuditSupportColumns().setLastModifiedOn( getAuditSupportColumns().getCreatedOn() );
		}

		@PreUpdate
		public void onUpdate() {
			if ( getAuditSupportColumns() == null ) {
				setAuditSupportColumns( new AuditSupportColumns() );
				getAuditSupportColumns().setCreatedOn( new Date() );
			}
			getAuditSupportColumns().setLastModifiedOn( new Date() );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public AuditSupportColumns getAuditSupportColumns() {
			return auditSupportColumns;
		}

		public void setAuditSupportColumns(AuditSupportColumns auditSupportColumns) {
			this.auditSupportColumns = auditSupportColumns;
		}

		public Date getCreatedOn() {
			return auditSupportColumns.getCreatedOn();
		}

		public void setCreatedOn(Date createdOn) {
			auditSupportColumns.setCreatedOn( createdOn );
		}

		public Date getLastModifiedOn() {
			return auditSupportColumns.getLastModifiedOn();
		}

		public void setLastModifiedOn(Date lastModifiedOn) {
			auditSupportColumns.setLastModifiedOn( lastModifiedOn );
		}
	}

	@Embeddable
	public static class AuditSupportColumns {
		private Date lastModifiedOn;
		@Column(updatable = false)
		private Date createdOn;

		public Date getLastModifiedOn() {
			return lastModifiedOn;
		}

		public void setLastModifiedOn(Date lastModifiedOn) {
			this.lastModifiedOn = lastModifiedOn;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}
	}

}
