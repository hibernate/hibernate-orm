package org.hibernate.test.join;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.persistence.*;

import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OuterJoinTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ A.class, B.class, C.class, D.class, Association.class };
	}


	@MappedSuperclass
	public static class BaseEntity {

		@Id
		@Column(name = "key_id")
		Long key;

		String value;

		@ManyToOne(optional = false)
		Association association;

		public BaseEntity() {
		}

		public BaseEntity(Long key, String value, Association association) {
			this.key = key;
			this.value = value;
			this.association = association;
		}
	}

	@Entity(name = "A")
	@Table(name = "a")
	public static class A extends BaseEntity {

		@OneToOne
		@PrimaryKeyJoinColumn(columnDefinition = "association_key", referencedColumnName = "key_id")
		private C cAssociationByKey;

		public A() {
		}

		public A(Long key, String value, Association association) {
			super(key, value, association);
		}
	}


	@Entity(name = "B")
	@Table(name = "b")
	public static class B extends BaseEntity {
		public B() {
		}

		public B(Long key, String value, Association association) {
			super(key, value, association);
		}
	}

	@Entity(name = "C")
	@Table(name = "c")
	public static class C extends BaseEntity {
		public C() {
		}

		public C(Long key, String value, Association association) {
			super(key, value, association);
		}
	}

	@Entity(name = "D")
	@Table(name = "d")
	public static class D extends BaseEntity {
		public D() {
		}

		public D(Long key, String value, Association association) {
			super(key, value, association);
		}

	}

	@Entity(name = "Association")
	@Table(name = "association")
	public static class Association {

		@Id
		@Column(name = "key_id")
		private Long key;

		private String value;

		public Association() {
		}

		public Association(Long key, String value) {
			this.key = key;
			this.value = value;
		}
	}

	@Before
	public void setUp() throws Exception {
		doInJPA( this::sessionFactory, em -> {
			Association association = new Association(1l, "association");
			em.merge(association);

			em.merge(new A(1L, "a", association));
			em.merge(new A(2L, "b", association));
			em.merge(new A(3L, "c", association));

			em.merge(new B(1L, "d", association));
			em.merge(new B(2L, "e", association));
			em.merge(new B(3L, "f", association));

			em.merge(new C(1L, "g", association));
			em.merge(new C(2L, "h", association));
			em.merge(new C(4L, "j", association));

			em.merge(new D(1L, "k", association));
			em.merge(new D(2L, "l", association));
			em.merge(new D(4L, "m", association));
		});
	}

	@Test
	public void testJoinOrderWithRightJoin() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key " +
					"RIGHT JOIN C c ON a.key = c.key " +
					"INNER JOIN D d ON d.key = c.key " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class)
					.getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	public void testJoinOrderWithRightNormalJoin() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key " +
					"RIGHT JOIN a.cAssociationByKey c " +
					"INNER JOIN D d ON d.key = c.key " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class)
					.getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	public void testJoinOrderWithRightJoinWithIdDereference() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key AND a.association.key = b.association.key " +
					"RIGHT JOIN C c ON a.key = c.key AND a.association.key = c.association.key " +
					"INNER JOIN D d ON d.key = c.key AND d.association.key = c.association.key " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class).getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	public void testJoinOrderWithRightNormalJoinWithIdDereference() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key AND a.association.key = b.association.key " +
					"RIGHT JOIN a.cAssociationByKey c ON a.key = c.key AND a.association.key = c.association.key " +
					"INNER JOIN D d ON d.key = c.key AND d.association.key = c.association.key " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class).getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	public void testJoinOrderWithRightJoinWithInnerImplicitJoins() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key,b.key,c.key,d.key) AS key, a.value AS aValue, b.value AS bValue, c.value AS cValue, d.value AS dValue " +
					"FROM A a JOIN a.association association_1 JOIN B b ON  (EXISTS (SELECT 1 FROM b.association _synth_subquery_0 WHERE a.key = b.key AND association_1.value = _synth_subquery_0.value))" +
					"RIGHT JOIN C c ON (EXISTS (SELECT 1 FROM c.association _synth_subquery_0 WHERE a.key = c.key AND association_1.value = _synth_subquery_0.value)) " +
					"JOIN c.association association_5 " +
					"JOIN D d ON (EXISTS (SELECT 1 FROM d.association _synth_subquery_0 WHERE d.key = c.key AND _synth_subquery_0.value = association_5.value))" +
					" ORDER BY COALESCE(a.key,b.key,c.key,d.key) ASC", Tuple.class).getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	@Ignore("Hibernate doesn't support implicit joins")
	public void testJoinOrderWithRightNormalJoinWithInnerImplicitJoins() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key AND a.association.value = b.association.value " +
					"RIGHT JOIN a.cAssociationByKey c ON a.key = c.key AND a.association.value = c.association.value " +
					"INNER JOIN D d ON d.key = c.key AND d.association.value = c.association.value " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class).getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	public void testJoinOrderWithRightJoinWithNonOptionalAssociationProjections() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key " +
					"RIGHT JOIN C c ON a.key = c.key " +
					"INNER JOIN D d ON d.key = c.key " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class).getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

	@Test
	public void testJoinOrderWithRightNormalJoinWithNonOptionalAssociationProjections() {
		doInJPA( this::sessionFactory, em -> {
			List<Tuple> resultList = em.createQuery("SELECT COALESCE(a.key, b.key, c.key, d.key), a.value, b.value, c.value, d.value " +
					"FROM A a " +
					"INNER JOIN B b ON a.key = b.key " +
					"RIGHT JOIN a.cAssociationByKey c ON a.key = c.key " +
					"INNER JOIN D d ON d.key = c.key " +
					"ORDER BY COALESCE(a.key, b.key, c.key, d.key) ASC", Tuple.class).getResultList();

			assertEquals(3, resultList.size());

			assertEquals("a", resultList.get(0).get(1));
			assertEquals("d", resultList.get(0).get(2));
			assertEquals("g", resultList.get(0).get(3));
			assertEquals("k", resultList.get(0).get(4));

			assertEquals("b", resultList.get(1).get(1));
			assertEquals("e", resultList.get(1).get(2));
			assertEquals("h", resultList.get(1).get(3));
			assertEquals("l", resultList.get(1).get(4));

			assertNull(resultList.get(2).get(1));
			assertNull(resultList.get(2).get(2));
			assertEquals("j", resultList.get(2).get(3));
			assertEquals("m", resultList.get(2).get(4));
		});
	}

}
