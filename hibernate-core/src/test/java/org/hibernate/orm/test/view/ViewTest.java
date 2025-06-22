/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.view;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = {ViewTest.Table.class, ViewTest.View.class, ViewTest.Summary.class})
public class ViewTest {

	@Test void test(SessionFactoryScope scope) {
		UUID id = scope.fromTransaction( s -> {
			Table t = new Table();
			t.quantity = 69.0;
			t.name = "Trompon";
			s.persist(t);
			return t.id;
		});
		scope.inSession( s -> {
			View v = s.find(View.class, id);
			assertNotNull(v);
			assertEquals("TROMPON", v.name);
		});
		scope.inSession( s -> {
			Summary summary = s.find(Summary.class, 1);
			assertNotNull(summary);
		});
	}

	@Entity(name = "Table")
	@jakarta.persistence.Table(name = "MyTable")
	static class Table {
		@Id @GeneratedValue
		UUID id;
		String name;
		double quantity;
	}

	@Entity(name = "View")
	@jakarta.persistence.Table(name = "MyView")
	@org.hibernate.annotations.View(query
			= "select id as uuid, upper(name) as name, quantity as amount from MyTable")
	static class View {
		@Id
		UUID uuid;
		String name;
		double amount;
	}

	@Entity(name = "Summary")
	@org.hibernate.annotations.View(query
			= "select 1 as id, sum(quantity) as total from MyTable")
	static class Summary {
		@Id
		int id;
		double total;
	}

}
