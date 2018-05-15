/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.merge;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotSame;

/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-12592")
@RunWith(BytecodeEnhancerRunner.class)
public class MergeDetachedCascadedCollectionInEmbeddableTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Heading.class, Grouping.class, Thing.class };
	}

	@Test
	public void testMergeDetached() {
		final Heading heading = doInHibernate( this::sessionFactory, session -> {
			Heading entity = new Heading();
			entity.name = "new";
			entity.setGrouping( new Grouping() );
			entity.getGrouping().getThings().add( new Thing() );
			session.save( entity );
			return entity;
		} );

		doInHibernate( this::sessionFactory, session -> {
			heading.name = "updated";
			Heading headingMerged = (Heading) session.merge( heading );
			assertNotSame( heading, headingMerged );
			assertNotSame( heading.grouping, headingMerged.grouping );
			assertNotSame( heading.grouping.things, headingMerged.grouping.things );
		} );
	}

	@Entity(name = "Heading")
	public static class Heading {
		private long id;
		private String name;
		private Grouping grouping;

		@Id
		@GeneratedValue
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Grouping getGrouping() {
			return grouping;
		}

		public void setGrouping(Grouping grouping) {
			this.grouping = grouping;
		}
	}

	@Embeddable
	public static class Grouping {
		private Set<Thing> things = new HashSet<>();

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
		@JoinColumn
		public Set<Thing> getThings() {
			return things;
		}

		public void setThings(Set<Thing> things) {
			this.things = things;
		}
	}

	@Entity(name = "Thing")
	public static class Thing {
		private long id;

		@Id
		@GeneratedValue
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
