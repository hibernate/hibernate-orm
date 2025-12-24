/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				UnownedOneToManyWithJoinFormulaTest.Person.class,
				UnownedOneToManyWithJoinFormulaTest.Group.class,
				UnownedOneToManyWithJoinFormulaTest.GroupAssociationEntity.class,
		}
)
@Jira("https://hibernate.atlassian.net/browse/HHH-11303")
public class UnownedOneToManyWithJoinFormulaTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
			Person person1 = new Person();
			person1.id = "abc1";
			s.persist(person1);
			Person person2 = new Person();
			person2.id = "abc2";
			s.persist(person2);
			Group group = new Group();
			group.id = "g1";
			s.persist(group);
			GroupAssociationEntity p1g1 = new GroupAssociationEntity();
			p1g1.id = "G1";
			p1g1.memberOf = "ABC1";
			p1g1.group = group;
			p1g1.person = person1;
			s.persist(p1g1);
			GroupAssociationEntity p2g1 = new GroupAssociationEntity();
			p2g1.id = "G1";
			p2g1.memberOf = "ABC2";
			p2g1.group = group;
			p2g1.person = person2;
			s.persist(p2g1);
		} );
	}

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
			Person person = s.find(Person.class, "abc1");
			List<GroupAssociationEntity> groups = person.groups;
			assertEquals(1, groups.size());
		} );
	}
	@Entity
	@Table(name = "groups")
	public static class Group {
		@Id
		@Column(name = "id")
		public String id;
		@OneToMany(mappedBy = "group")
		public List<GroupAssociationEntity> persons;
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Group group = (Group) o;
			return Objects.equals(id, group.id);
		}
		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}
	@Entity
	@Table(name = "group_association")
	@IdClass(GroupAssociationKey.class)
	public static class GroupAssociationEntity {
		@Id
		public String id;
		@Id
		public String memberOf;
		@ManyToOne
		@JoinFormula( "UPPER(id)" )
		public Group group;
		@ManyToOne
		@JoinFormula( "UPPER(memberOf)" )
		public Person person;
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			GroupAssociationEntity that = (GroupAssociationEntity) o;
			return Objects.equals(id, that.id) &&
				Objects.equals(memberOf, that.memberOf);
		}
		@Override
		public int hashCode() {
			return Objects.hash(id, memberOf);
		}
		public Group getGroup() {
			return group;
		}
		public Person getPerson() {
			return person;
		}
	}
	public static class GroupAssociationKey implements Serializable {
		public String memberOf;
		public String id;
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			GroupAssociationKey that = (GroupAssociationKey) o;
			return Objects.equals(memberOf, that.memberOf) &&
				Objects.equals(id, that.id);
		}
		@Override
		public int hashCode() {
			return Objects.hash(memberOf, id);
		}
	}

	@Entity
	@Table(name = "person")
	public static class Person {
		@Id
		@Column(name = "id")
		public  String id;
		@OneToMany(mappedBy = "person")
		public List<GroupAssociationEntity> groups;
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Person person = (Person) o;
			return Objects.equals(id, person.id);
		}
		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}

}
