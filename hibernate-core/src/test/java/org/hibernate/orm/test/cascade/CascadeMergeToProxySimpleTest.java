/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;

@JiraKey(value = "HHH-13590")
@DomainModel(
		annotatedClasses = {
				CascadeMergeToProxySimpleTest.AbstractEntity.class,
				CascadeMergeToProxySimpleTest.Event.class,
				CascadeMergeToProxySimpleTest.Project.class
		}
)
@SessionFactory
public class CascadeMergeToProxySimpleTest {
	protected static final Random RANDOM_GENERATOR = new Random();

	@Test
	public void test(SessionFactoryScope scope) {
		final Event root = (Event) mergeEntity( scope, new Event( generateBId(), new Project( generateBId() ) ) );
		Event rootFromDB = (Event) mergeEntity( scope, root );

		assertNotNull( rootFromDB );
	}

	private Object mergeEntity(SessionFactoryScope scope, Object entity) {
		return scope.fromTransaction( session -> session.merge( entity ) );
	}

	private String generateBId() {
		return UUID.nameUUIDFromBytes(
				( Long.toString( System.currentTimeMillis() ) + RANDOM_GENERATOR.nextInt() )
						.getBytes()
		).toString();
	}

	@MappedSuperclass
	public static class AbstractEntity {

		@Id
		@GeneratedValue
		@Column(name = "id")
		protected Long objectID;

		@Column(nullable = false, unique = true, length = 36)
		private String bID;

		protected AbstractEntity() {
		}

		protected AbstractEntity(String bId) {
			this.bID = bId;
		}

		public long getObjectID() {
			return objectID;
		}

		@Override
		public String toString() {
			return String.format( "%s[id=%d]", getClass().getSimpleName(), getObjectID() );
		}

		public String getBID() {
			return bID;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || !this.getClass().isInstance( o ) ) {
				return false;
			}

			AbstractEntity that = (AbstractEntity) o;

			return bID != null ? bID.equals( that.bID ) : that.bID == null;
		}

		@Override
		public int hashCode() {
			return bID != null ? bID.hashCode() : 0;
		}
	}

	@Entity(name = "Event")
	public static class Event extends AbstractEntity {

		@OneToMany(targetEntity = Event.class, cascade = { CascadeType.ALL }, orphanRemoval = true, fetch = FetchType.LAZY)
		private Set<Event> children = new HashSet<>();

		@ManyToOne(targetEntity = Project.class, fetch = FetchType.LAZY, cascade = {
				CascadeType.PERSIST,
				CascadeType.MERGE,
				CascadeType.REFRESH
		})
		private Project project;

		public Event() {
			//framework purpose
		}

		public Event(String bid, Project project) {
			super( bid );
			setProject( project );
		}

		public void setProject(Project project) {
			this.project = project;
		}

		public Project getProject() {
			return project;
		}

		public Set<Event> getChildren() {
			return children;
		}

		public void addChild(Event event) {
			assert event != null;
			this.children.add( event );
		}

	}

	@Entity(name = "Project")
	public static class Project extends AbstractEntity {

		Project() {
		}

		Project(String bId) {
			super( bId );
		}

	}
}
