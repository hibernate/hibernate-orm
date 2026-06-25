/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.join;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses =
		{FilterWithJoinManyToManyTest.Parent.class,
		FilterWithJoinManyToManyTest.Child.class})
class FilterWithJoinManyToManyTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent euroParent = new Parent();
			euroParent.region = "Europe";
			session.persist( euroParent );
			Child euroChild1 = new Child();
			euroChild1.text = "Hello World";
			euroChild1.region = "Europe";
			session.persist( euroChild1 );
			Child euroChild2 = new Child();
			euroChild2.text = "Hello World";
			euroChild2.region = "Europe";
			session.persist( euroChild2 );
			Child asianChildOfEuro = new Child();
			asianChildOfEuro.text = "Gets filtered";
			asianChildOfEuro.region = "Asia";
			session.persist( asianChildOfEuro );
			euroParent.children.add( euroChild1 );
			euroParent.children.add( euroChild2 );
			euroParent.children.add( asianChildOfEuro );

			Parent asianParent = new Parent();
			asianParent.region = "Asia";
			session.persist( asianParent );
			Child asianChild = new Child();
			asianChild.text = "Goodbye World";
			asianChild.region = "Asia";
			session.persist( asianChild );
			asianParent.children.add( asianChild );
		} );
		scope.inTransaction( session -> {
			session.enableFilter( "regionFilter" )
					.setParameter( "region", "Europe" )
					.validate();
			var children =
					session.createQuery( "from Child", Child.class )
							.getResultList();
			assertEquals( 4, children.size() );
			var europeanChildren =
					session.createQuery( "from Child where region='Europe'", Child.class )
							.getResultList();
			assertEquals( 2, europeanChildren.size() );
			// only two of the children are in the Europe region; the third should be filtered out
			assertEquals( 2, session.find( Parent.class, 1L ).children.size() );
			children =
					session.createQuery( "select c from Parent p left join p.children c", Child.class )
							.getResultList();
			// only two of the children are in the Europe region; the third should be filtered out
			assertEquals( 2, children.size() );
		} );
	}

	@FilterDef(name = "regionFilter",
			parameters = @ParamDef(name = "region", type = String.class),
			defaultCondition = "region = :region")
	@Filter(name = "regionFilter")
	@Entity(name = "Parent")
	@Table(name = "parent")
	static class Parent {
		@GeneratedValue @Id long id;
		String region;
		@Filter(name = "regionFilter",
				tableName = "child_region",
				joinColumns = @JoinColumn(name = "id",
						referencedColumnName = "child_id"))
		@ManyToMany
		Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	@Table(name = "child")
	@SecondaryTable(name="child_region",
			pkJoinColumns = @PrimaryKeyJoinColumn(name="child_id",
					referencedColumnName="id"))
	static class Child {
		@GeneratedValue @Id long id;
		String text;
		@Column(table = "child_region")
		String region;
	}
}
