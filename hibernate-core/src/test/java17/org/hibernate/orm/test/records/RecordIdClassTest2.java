/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.records;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

@DomainModel( annotatedClasses = {
		RecordIdClassTest2.MyParentEntity.class,
		RecordIdClassTest2.MyChildEntity.class} )
@SessionFactory
public class RecordIdClassTest2 {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			MyParentEntity ue = new MyParentEntity("hello");
			MyChildEntity uae = new MyChildEntity(ue, "world");
			ue.children.add(uae);
			s.persist(ue);
		});
	}

	public record MyRecord(Long code, String qualifier) {}

	@Entity
	@IdClass(MyRecord.class)
	public static class MyChildEntity {

		@Id
		Long code;

		@Id
		String qualifier;

		String text;

		@ManyToOne
		@MapsId("code")
		private MyParentEntity parent;

		public MyChildEntity(MyParentEntity parent, String qualifier) {
			this.parent = parent;
			this.qualifier = qualifier;
		}

		MyChildEntity() {
		}
	}

	@Entity
	public static class MyParentEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long code;

		String description;

		@OneToMany(
				cascade = {PERSIST, MERGE, REMOVE},
				mappedBy = "parent",
				orphanRemoval = true)
		private Set<MyChildEntity> children = new HashSet<>();

		public MyParentEntity(String description) {
			this.description = description;
		}

		MyParentEntity() {
		}
	}
}
