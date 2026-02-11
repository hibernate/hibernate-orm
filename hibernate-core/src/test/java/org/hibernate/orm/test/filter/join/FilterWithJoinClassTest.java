/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.join;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses =
		{FilterWithJoinClassTest.Parent.class,
		FilterWithJoinClassTest.Child.class})
class FilterWithJoinClassTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent euroParent = new Parent();
			euroParent.region = "Europe";
			Child euroChild1 = new Child();
			euroChild1.text = "Hello World";
			session.persist( euroParent );
			euroChild1.parentId = euroParent.id;
			session.persist( euroChild1 );
			Child euroChild2 = new Child();
			euroChild2.text = "Hello World";
			session.persist( euroParent );
			euroChild2.parentId = euroParent.id;
			session.persist( euroChild2 );

			Parent asianParent = new Parent();
			asianParent.region = "Asia";
			Child asianChild = new Child();
			asianChild.text = "Goodbye World";
			session.persist( asianParent );
			asianChild.parentId = asianParent.id;
			session.persist( asianChild );
		} );
		scope.inTransaction( session -> {
			session.enableFilter( "regionFilter" )
					.setParameter( "region", "Europe" )
					.validate();
			var children =
					session.createQuery( "from Child", Child.class )
							.getResultList();
			assertEquals( 2, children.size() );
			children =
					session.createQuery( "select c from Parent p left join Child c on p.id = c.parentId", Child.class )
							.getResultList();
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
	}

	@Filter(name = "regionFilter",
			condition = "region = :region",
			join = @Filter.Join(tableName = "parent",
					joinColumns = @JoinColumn(name = "parentId",
							referencedColumnName = "id")))
	@Entity(name = "Child")
	@Table(name = "child")
	static class Child {
		@GeneratedValue @Id long id;
		String text;
		long parentId;
	}
}
