/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.Collections;
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
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.Version;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-13590")
@DomainModel(
		annotatedClasses = {
				CascadeMergeToProxyEntityCopyAllowedTest.AbstractEntity.class,
				CascadeMergeToProxyEntityCopyAllowedTest.Event.class,
				CascadeMergeToProxyEntityCopyAllowedTest.Project.class,
				CascadeMergeToProxyEntityCopyAllowedTest.Speaker.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "allow"
				)
		}
)
public class CascadeMergeToProxyEntityCopyAllowedTest {
	private Project defaultProject;

	@Test
	public void test(SessionFactoryScope scope) {
		final Event root = (Event) persistEntity( scope, new Event( null, defaultProject ) );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		Event rootFromDB = scope.fromTransaction(
				session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e LEFT JOIN FETCH e.speakers LEFT JOIN FETCH e.children LEFT JOIN FETCH e.project WHERE e.objectID = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", root.getObjectID() );

					return eventTypedQuery.getSingleResult();

				}
		);
		statementInspector.assertExecutedCount( 1 );
		statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 4 );
		assertNotNull( rootFromDB );
		assertEquals( 0, rootFromDB.getChildren().size() );
		assertEquals( 0, rootFromDB.getSpeakers().size() );
		assertEquals( root, rootFromDB );

		Speaker speaker = (Speaker) persistEntity( scope, new Speaker( defaultProject ) );
		final long speakerId = speaker.getObjectID();

		speaker = scope.fromTransaction(
				session ->
						session.find( Speaker.class, speakerId )
		);
		assertNotNull( speaker );

		Event child = new Event( rootFromDB, defaultProject );
		child.addSpeaker( speaker );

		rootFromDB = (Event) persistEntity( scope, rootFromDB );
		final long rootFromDBId = rootFromDB.getObjectID();
		rootFromDB = scope.fromTransaction(
				session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e LEFT JOIN FETCH e.speakers LEFT JOIN FETCH e.children LEFT JOIN FETCH e.project WHERE e.objectID = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", rootFromDBId );

					return eventTypedQuery.getSingleResult();

				}
		);
		assertNotNull( rootFromDB );
		assertEquals( 1, rootFromDB.getChildren().size() );
		assertEquals( 0, rootFromDB.getSpeakers().size() );
	}

	private Object persistEntity(SessionFactoryScope scope, Object entity) {
		return scope.fromTransaction(
				session -> {
					Object mergedEntity = session.merge( entity );
					session.persist( mergedEntity );
					session.flush();
					return mergedEntity;
				}
		);
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		Long objectId = scope.fromTransaction(
				session -> {
					Project project = (Project) session.merge( new Project() );
					session.persist( project );
					session.flush();
					return project.getObjectID();
				}
		);
		scope.inTransaction(
				session -> {
					TypedQuery<Project> projectTypedQuery = session.createQuery(
							"SELECT p FROM Project p WHERE p.objectID = :oid",
							Project.class
					);

					projectTypedQuery.setParameter( "oid", objectId );

					defaultProject = projectTypedQuery.getSingleResult();
				}
		);
	}

	@MappedSuperclass
	public static class AbstractEntity {

		static long INVALID_OBJECT_ID = -1;

		@Transient
		protected static final Random RANDOM_GENERATOR = new Random();

		@Id
		@GeneratedValue
		@Column(name = "id")
		protected Long objectID = INVALID_OBJECT_ID;

		@Version
		private int version;

		@Column(nullable = false, unique = true, length = 36)
		private final String bID;

		protected AbstractEntity() {
			bID = UUID.nameUUIDFromBytes(
					( Long.toString( System.currentTimeMillis() ) + RANDOM_GENERATOR.nextInt() )
							.getBytes()
			).toString();
		}

		public int getVersion() {
			return version;
		}

		public long getObjectID() {
			return objectID;
		}

		public static boolean isValidObjectID(long id) {
			return ( id > 0 && id != AbstractEntity.INVALID_OBJECT_ID );
		}

		public boolean isPersistent() {
			return isValidObjectID( getObjectID() );
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
			if ( o == null || getClass() != o.getClass() ) {
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
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Event extends AbstractEntity {

		@ManyToOne(targetEntity = Event.class, fetch = FetchType.EAGER)
		private Event parent;

		@OneToMany(targetEntity = Event.class, cascade = { CascadeType.ALL }, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "parent")
		private Set<Event> children = new HashSet<>();

		@ManyToOne(targetEntity = Project.class, fetch = FetchType.LAZY, cascade = {
				CascadeType.PERSIST,
				CascadeType.MERGE,
				CascadeType.REFRESH
		})
		private Project project;

		@ManyToMany(targetEntity = Speaker.class,fetch = FetchType.LAZY, cascade = {
				CascadeType.PERSIST,
				CascadeType.REFRESH
		})
		private Set<Speaker> speakers = new HashSet<>();


		public Event() {
			//framework purpose
		}

		public Event(Event parent, Project project) {
			setParent( parent );
			setProject( project );

			if ( parent == null ) {
				//nothing to do here, Event has no parent
			}
			else {
				parent.addChild( this );
			}
		}

		public void setParent(Event parent) {
			this.parent = parent;
		}

		public void setProject(Project project) {
			this.project = project;
		}

		public Event getParent() {
			return parent;
		}

		public Project getProject() {
			return project;
		}

		public Set<Speaker> getSpeakers() {
			return Collections.unmodifiableSet( speakers );
		}

		public Set<Event> getChildren() {
			return Collections.unmodifiableSet( children );
		}

		public void addSpeaker(Speaker speaker) {
			assert speaker != null;
			this.speakers.add( speaker );
		}

		public void addChild(Event event) {
			assert event != null;
			this.children.add( event );
			event.setParent( this );
		}

	}

	@Entity(name = "Project")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Project extends AbstractEntity {

	}

	@Entity(name = "Speaker")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Speaker extends AbstractEntity {


		@ManyToOne(targetEntity = Project.class, fetch = FetchType.LAZY,
				cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
		private Project project;

		public Speaker() {

		}

		public Speaker(Project project) {
			this.project = project;
		}

		public Project getProject() {
			return project;
		}
	}
}
