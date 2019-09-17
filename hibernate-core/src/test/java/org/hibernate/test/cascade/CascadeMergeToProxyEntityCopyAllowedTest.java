/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.Version;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestForIssue( jiraKey = "HHH-13590")
public class CascadeMergeToProxyEntityCopyAllowedTest extends BaseCoreFunctionalTestCase {
	private Project defaultProject;


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				AbstractEntity.class,
				Event.class,
				Project.class,
				Speaker.class
		};
	}

	@Test
	public void test() {
		final Event root = (Event) persistEntity( new Event( null, defaultProject  ) );

		Event rootFromDB = doInHibernate(
				this::sessionFactory, session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e LEFT JOIN FETCH e.speakers LEFT JOIN FETCH e.children LEFT JOIN FETCH e.project WHERE e.objectID = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", root.getObjectID() );

					return eventTypedQuery.getSingleResult();

				}
		);
		assertNotNull( rootFromDB );
		assertEquals(0, rootFromDB.getChildren().size());
		assertEquals( 0, rootFromDB.getSpeakers().size() );
		assertEquals( root, rootFromDB );

		Speaker speaker = (Speaker) persistEntity( new Speaker(defaultProject) );
		final long speakerId = speaker.getObjectID();

		speaker = doInHibernate(
				this::sessionFactory, session -> {
					return session.find( Speaker.class, speakerId );
				}
		);
		assertNotNull( speaker );

		Event child = new Event(rootFromDB, defaultProject);
		child.addSpeaker( speaker );

		rootFromDB = (Event) persistEntity( rootFromDB );
		final long rootFromDBId = rootFromDB.getObjectID();
		rootFromDB = doInHibernate(
				this::sessionFactory, session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e LEFT JOIN FETCH e.speakers LEFT JOIN FETCH e.children LEFT JOIN FETCH e.project WHERE e.objectID = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", rootFromDBId );

					return eventTypedQuery.getSingleResult();

				}
		);
		assertNotNull( rootFromDB );
		assertEquals(1, rootFromDB.getChildren().size());
		assertEquals(0, rootFromDB.getSpeakers().size());

	}

	private Object persistEntity(Object entity ) {
		return doInHibernate(
				this::sessionFactory, session -> {
					Object mergedEntity = session.merge( entity );
					session.persist( mergedEntity );
					session.flush();
					return mergedEntity;
				}
		);
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, "allow" );
	}

	@Before
	public void setupData() {
		Long objectId = doInHibernate(
				this::sessionFactory, session -> {
					Project project = (Project) session.merge( new Project() );
					session.persist( project );
					session.flush();
					return project.getObjectID();
				}
		);
		doInHibernate(
				this::sessionFactory, session -> {
					TypedQuery<Project> projectTypedQuery = session.createQuery("SELECT p FROM Project p WHERE p.objectID = :oid", Project.class);

					projectTypedQuery.setParameter("oid", objectId);

					defaultProject = projectTypedQuery.getSingleResult();
				}
		);
	}

	@MappedSuperclass
	public static class AbstractEntity {

		static long INVALID_OBJECT_ID = -1 ;

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
			return (id > 0 && id != AbstractEntity.INVALID_OBJECT_ID);
		}

		public boolean isPersistent() {
			return isValidObjectID(getObjectID());
		}

		@Override
		public String toString() {
			return String.format("%s[id=%d]", getClass().getSimpleName(), getObjectID());
		}

		public String getBID() {
			return bID;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			AbstractEntity that = (AbstractEntity) o;

			return bID != null ? bID.equals(that.bID) : that.bID == null;

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

		@OneToMany(targetEntity = Event.class, cascade = { CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "parent")
		private Set<Event> children = new HashSet<>();

		@ManyToOne(targetEntity = Project.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
		private Project project;

		@ManyToMany(targetEntity = Speaker.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH})
		private Set<Speaker> speakers = new HashSet<>();


		public Event() {
			//framework purpose
		}

		public Event(Event parent, Project project) {
			setParent(parent);
			setProject(project);

			if (parent == null) {
				//nothing to do here, Event has no parent
			} else {
				parent.addChild(this);
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
			this.speakers.add(speaker);
		}

		public void addChild(Event event) {
			assert event != null;
			this.children.add(event);
			event.setParent(this);
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
				cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
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
