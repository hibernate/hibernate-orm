/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.misc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

@DomainModel(annotatedClasses = {Misc1Test.EntityA.class, Misc1Test.EntityB.class})
@SessionFactory
@JiraKey(value = "HHH-15355")
public class Misc1Test {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(s->{
			EntityA a = new EntityA();
			EntityB b = new EntityB();
			a.entityB = b;
			b.entityA = a;
			s.persist(a);
			s.persist(b);

			s.createQuery("from B join entityA", EntityB.class).getSingleResult();
		});
	}

	@Entity(name = "A")
	public static class EntityA implements Serializable {
		@Id long entityBKey;
		@Id boolean flag;
		@OneToOne(mappedBy = "entityA")
		public EntityB entityB;
	}

	@Entity(name = "B")
	public static class EntityB implements Serializable {
		@Id long entityBKey;
		@Id boolean flag;
		@OneToOne
		@JoinColumn(name = "entityBKey", referencedColumnName = "entityBKey", insertable = false, updatable = false)
		@JoinColumn(name = "flag", referencedColumnName = "flag", insertable = false, updatable = false)
		public EntityA entityA;
	}
}
