/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.thisalias;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = HQLThisTest.This.class)
public class HQLThisTest {
	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction(s -> s.persist(new This("gavin")));
		scope.inSession(s -> {
			s.createSelectionQuery("select this.name from This this where this.name = 'gavin'").getSingleResult();
			s.createSelectionQuery("from This where this.name = 'gavin'").getSingleResult();
			s.createSelectionQuery("select this.name from This order by this.name").getSingleResult();
			s.createSelectionQuery("select count(this) from This").getSingleResult();
			s.createSelectionQuery("select id(this) from This").getSingleResult();
		});
	}
	@Entity(name="This")
	static class This {
		@Id @GeneratedValue
		long id;
		@Basic(optional = false)
		String name;

		This(String name) {
			this.name = name;
		}

		This() {
		}
	}
}
