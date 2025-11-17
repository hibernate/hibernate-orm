/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OrderBy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		annotatedClasses = {
				OrderByColumnNameTest.Product.class,
				OrderByColumnNameTest.Widgets.class,
				OrderByColumnNameTest.Widget1.class,
				OrderByColumnNameTest.Widget2.class,
		}
)
@SessionFactory
public class OrderByColumnNameTest {

	@Test
	public void testOrderByName(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
						Product p = new Product();
						HashSet<Widgets> set = new HashSet<>();

						Widgets widget = new Widgets();
						widget.setName( "hammer" );
						set.add( widget );
						session.persist( widget );

						widget = new Widgets();
						widget.setName( "axel" );
						set.add( widget );
						session.persist( widget );

						widget = new Widgets();
						widget.setName( "screwdriver" );
						set.add( widget );
						session.persist( widget );

						p.setWidgets( set );
						session.persist( p );
						session.getTransaction().commit();

						session.beginTransaction();
						session.clear();
						p = session.get( Product.class, p.getId() );
						assertTrue( p.getWidgets().size() == 3, "has three Widgets" );
						Iterator iter = p.getWidgets().iterator();
						assertEquals( "axel", ( (Widgets) iter.next() ).getName() );
						assertEquals( "hammer", ( (Widgets) iter.next() ).getName() );
						assertEquals( "screwdriver", ( (Widgets) iter.next() ).getName() );
				}
		);
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		@GeneratedValue
		private Integer id;

		@ElementCollection
		@OrderBy("name_1 ASC")
		private Set<Widgets> widgets;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Set<Widgets> getWidgets() {
			return widgets;
		}

		public void setWidgets(Set<Widgets> widgets) {
			this.widgets = widgets;
		}
	}

	@Entity(name = "Widgets")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Widgets {
		private String name;
		private int id;

		public Widgets() {

		}

		@Column(name = "name_1")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}

	@Entity(name = "Widget1")
	public static class Widget1 extends Widgets {
		private String name1;
	}

	@Entity(name = "Widget2")
	public static class Widget2 extends Widgets {
		private String name2;
	}

}
