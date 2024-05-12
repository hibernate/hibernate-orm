/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.hibernate.orm.test.bytecode.enhancement.merge.MergeDetachedNonCascadedCollectionInEmbeddableTest.*;
import static org.junit.Assert.assertNotSame;

import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
@JiraKey("HHH-12637")
@DomainModel(
		annotatedClasses = {
				Heading.class, Grouping.class, Thing.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MergeDetachedNonCascadedCollectionInEmbeddableTest {

	@Test
	public void testMergeDetached(SessionFactoryScope scope) {
		final Heading heading = scope.fromTransaction( session -> {
			Heading entity = new Heading();
			entity.name = "new";
			entity.setGrouping( new Grouping() );
			Thing thing = new Thing();
			entity.getGrouping().getThings().add( thing );
			session.save( thing );
			session.save( entity );
			return entity;
		} );

		scope.inTransaction( session -> {
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

		@OneToMany(fetch = FetchType.LAZY)
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
