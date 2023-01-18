/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {JoinColumnOrFormula2Test.A.class, JoinColumnOrFormula2Test.D.class})
public class JoinColumnOrFormula2Test {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			A a = new A();
			a.id = 3;
			a.dId = 5;
			D d = new D();
			d.id = 5;
			s.persist(a);
			s.persist(d);
			a.ds.add(d);
		});
		scope.inSession(s -> {
			Set<D> ds = s.get(A.class, 3).ds;
			assertEquals(1, ds.size());
		});
	}

	@Entity( name = "A" )
	@Table( name = "A" )
	public static class A {
		@Id
		@Column( name = "aid")
		public Integer id;

		@Column( name = "did")
		public Integer dId;

		@ManyToMany
		@JoinFormula(value = "A_aid+2", referencedColumnName = "did")
		Set<D> ds = new HashSet<>();
	}

	@Entity( name = "D" )
	@Table( name = "D" )
	public static class D {
		@Id
		@Column( name = "did")
		public Integer id;
	}
}
