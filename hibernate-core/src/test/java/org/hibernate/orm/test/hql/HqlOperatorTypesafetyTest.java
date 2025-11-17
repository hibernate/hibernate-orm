/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = HqlOperatorTypesafetyTest.Book.class)
public class HqlOperatorTypesafetyTest {
	@Test void testOperatorTyping(SessionFactoryScope scope) {
		scope.inSession( s -> {
			// these should succeed
			s.createSelectionQuery("from Book where title = 'Hibernate'").getResultList();
			s.createSelectionQuery("from Book where title > ''").getResultList();
			s.createSelectionQuery("select edition + 1 from Book").getResultList();
			s.createSelectionQuery("select title || '!' from Book").getResultList();
			try {
				s.createSelectionQuery("from Book where title = 1").getResultList();
				fail();
			}
			catch (SemanticException se) {}
			try {
				s.createSelectionQuery("from Book where title > 1").getResultList();
				fail();
			}
			catch (SemanticException se) {}
			try {
				s.createSelectionQuery("select title + 1 from Book").getResultList();
				fail();
			}
			catch (SemanticException se) {}
		});
	}

	@Test void testSubselectTyping(SessionFactoryScope scope) {
		scope.inSession( s -> {
			// these should succeed
			s.createSelectionQuery("from Book where title in (select title from Book)").getResultList();
			s.createSelectionQuery("from Book where title = any (select title from Book)").getResultList();

			// test tuple length errors
			try {
				s.createSelectionQuery("from Book where title = any (select title, isbn from Book)").getResultList();
				fail();
			}
			catch (SemanticException se) {}
			try {
				s.createSelectionQuery("from Book where title = every (select title, isbn from Book)").getResultList();
				fail();
			}
			catch (SemanticException se) {}
			try {
				s.createSelectionQuery("from Book where title in (select title, isbn from Book)").getResultList();
				fail();
			}
			catch (SemanticException se) {}

			// test typing errors
			try {
				s.createSelectionQuery("from Book where 1 = any (select title from Book)").getResultList();
				fail();
			}
			catch (SemanticException se) {}
			try {
				s.createSelectionQuery("from Book where 1 = every (select title from Book)").getResultList();
				fail();
			}
			catch (SemanticException se) {}
			try {
				s.createSelectionQuery("from Book where 1 in (select title from Book)").getResultList();
				fail();
			}
			catch (SemanticException se) {}
		});
	}

	@Entity(name = "Book")
	static class Book {
		@Id String isbn;
		String title;
		int edition;
	}
}
