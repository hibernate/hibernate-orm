/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cascade;

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
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

@TestForIssue( jiraKey = "HHH-13590")
public class CascadeMergeToProxySimpleTest extends BaseCoreFunctionalTestCase {
	protected static final Random RANDOM_GENERATOR = new Random();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				AbstractEntity.class,
				Event.class,
				Project.class
		};
	}

	@Test
	public void test() {
		final Event root = (Event) mergeEntity( new Event( generateBId(), new Project( generateBId() ) ) );
		Event rootFromDB = (Event) mergeEntity( root );

		assertNotNull( rootFromDB );
	}

	private Object mergeEntity(Object entity) {
		return doInHibernate(
				this::sessionFactory, session -> {
					return session.merge( entity );
				}
		);
	}

	private String generateBId() {
		return UUID.nameUUIDFromBytes(
				( Long.toString( System.currentTimeMillis() ) + RANDOM_GENERATOR.nextInt() )
						.getBytes()
		).toString();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
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
			return String.format("%s[id=%d]", getClass().getSimpleName(), getObjectID());
		}

		public String getBID() {
			return bID;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if ( o == null || !this.getClass().isInstance( o ) ) return false;

			AbstractEntity that = (AbstractEntity) o;

			return bID != null ? bID.equals( that.bID) : that.bID == null;
		}

		@Override
		public int hashCode() {
			return bID != null ? bID.hashCode() : 0;
		}
	}

	@Entity(name = "Event")
	public static class Event extends AbstractEntity {

		@OneToMany(targetEntity = Event.class, cascade = { CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
		private Set<Event> children = new HashSet<>();

		@ManyToOne(targetEntity = Project.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
		private Project project;

		public Event() {
			//framework purpose
		}

		public Event(String bid, Project project) {
			super( bid );
			setProject(project);
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
			this.children.add(event);
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
