/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				HierarchyOrderTest.DerOA.class,
				HierarchyOrderTest.DerDA.class,
				HierarchyOrderTest.DerDB.class,
				HierarchyOrderTest.DerOB.class,
				HierarchyOrderTest.BaseD.class,
				HierarchyOrderTest.BaseO.class
		}
)
@SessionFactory
class HierarchyOrderTest {

	private EntityManagerFactory emf;
	private DerOA deroa;
	private DerOB derob;

	@BeforeEach
	void setUp() {
		DerDB derba1 = new DerDB( 5 );
		DerDA derda1 = new DerDA( "1", "abase" );
		deroa = new DerOA( derda1 );
		derob = new DerOB( derba1 );
//		emf = buildEntityManagerFactory();
	}

	@Test
	void testBaseProperty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.getTransaction().begin();
			em.persist( deroa );
			em.persist( derob );
			em.getTransaction().commit();
			Integer ida = deroa.getId();
			Integer idb = derob.getId();
			em.clear();
			TypedQuery<DerOA> qa = em.createQuery( "select o from DerOA o where o.id =:id", DerOA.class );
			qa.setParameter( "id", ida );
			DerOA deroain = qa.getSingleResult();
			assertEquals( "abase", deroain.derda.baseprop );
		} );
	}

	@Test
	void testDerivedProperty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.getTransaction().begin();
			em.persist( deroa );
			em.persist( derob );
			em.getTransaction().commit();
			Integer idb = derob.getId();
			em.clear();

			TypedQuery<DerOB> qb = em.createQuery( "select o from DerOB o where o.id =:id", DerOB.class );
			qb.setParameter( "id", idb );
			DerOB derobin = qb.getSingleResult();
			assertNotNull( derobin );
			assertEquals( 5, derobin.derdb().b );
		} );
	}

	/*
	 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
	 */
	@Entity(name = "DerOA")
	public static class DerOA extends BaseO {
		public DerOA(DerDA derda) {
			this.derda = derda;
		}

		@Embedded
		//   @AttributeOverrides({
		//         @AttributeOverride(name="a",column = @Column(name = "da"))
		//   })
		public BaseD derda;

		public DerOA() {

		}
	}

	/*
	 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
	 */
	@Embeddable
	public static class DerDB extends BaseD {
		public DerDB(int b) {
			this.b = b;
		}

		public int b;

		public DerDB() {

		}
	}

	/*
	 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
	 */
	@Embeddable
	public static class DerDA extends BaseD {
		public DerDA(String a, String bprop) {
			super( bprop );
			this.a = a;
		}

		public String a;

		public DerDA() {

		}
	}

	/*
	 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
	 */
	@Embeddable
	public abstract static class BaseD { //TODO would really like this to be abstract
		public String baseprop;

		public BaseD(String baseprop) {
			this.baseprop = baseprop;
		}

		public BaseD() {

		}

		public String getBaseprop() {
			return baseprop;
		}

	}

	@Entity(name = "BaseO")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class BaseO {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}
	}

	/*
	 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
	 */
	@Entity(name = "DerOB")
	public static class DerOB extends BaseO {
		public DerOB(DerDB derdb) {
			this.derdb = derdb;
		}

		@Embedded
		BaseD derdb;

		public DerOB() {

		}

		public DerDB derdb() {
			return (DerDB) derdb;
		}
	}
}
