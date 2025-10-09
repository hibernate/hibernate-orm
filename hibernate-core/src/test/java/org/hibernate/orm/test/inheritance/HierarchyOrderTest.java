/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TypedQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings({"JUnitMalformedDeclaration", "removal"})
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
	private DerOA deroa;
	private DerOB derob;

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		DerDB derba1 = new DerDB( 5 );
		DerDA derda1 = new DerDA( "1", "abase" );
		deroa = new DerOA( derda1 );
		derob = new DerOB( derba1 );
		factoryScope.inTransaction( (em) -> {
			em.persist( deroa );
			em.persist( derob );
		} );
	}

	@Test
	void testBaseProperty(SessionFactoryScope factoryScope) {
		factoryScope.inSession( em -> {
			TypedQuery<DerOA> qa = em.createQuery( "select o from DerOA o where o.id =:id", DerOA.class );
			qa.setParameter( "id", deroa.getId() );
			DerOA deroain = qa.getSingleResult();
			assertEquals( "abase", deroain.derda.baseprop );
		} );
	}

	@Test
	void testDerivedProperty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<DerOB> qb = em.createQuery( "select o from DerOB o where o.id =:id", DerOB.class );
			qb.setParameter( "id", derob.getId() );
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
