/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.misc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

@DomainModel(annotatedClasses = {Misc0Test.EntityA.class, Misc0Test.EntityB.class})
@SessionFactory
@JiraKey(value = "HHH-15355")
public class Misc0Test {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(s->{
			EntityA a = new EntityA();
			EntityB b = new EntityB();
			a.flag = 1;
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
		@Id int flag;
		@OneToOne(mappedBy = "entityA")
		public EntityB entityB;
	}

	@Entity(name = "B")
	public static class EntityB implements Serializable {
		@Id long entityBKey;
		@OneToOne

		@JoinColumnOrFormula(column = @JoinColumn(name = "entityBKey", referencedColumnName = "entityBKey", insertable = false, updatable = false))
		@JoinColumnOrFormula(formula = @JoinFormula(value = "1", referencedColumnName = "flag"))
		public EntityA entityA;
	}
}
