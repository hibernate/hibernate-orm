/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinwithoutancestor;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-12993")
public class OmitAncestorJoinWhenCommonSecondaryTablePresentTest extends OmitAncestorTestCase {

	private static final String SECONDARY_TABLE_NAME = "secondary_table";

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { A.class, SubA.class, B.class, SubB.class, C.class };
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void cleanupTestData() throws Exception {
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "from A", A.class ).list().forEach( s::remove );
			s.createQuery( "from B", B.class ).list().forEach( s::remove );
			s.createQuery( "from C", C.class ).list().forEach( s::remove );
		} );
		super.cleanupTestData();
	}

	@Test
	public void shouldNotReturnSecondaryTableValueForSubB() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			SubA subA = new SubA( 1L );
			subA.setValSubA( "valSubA" );
			subA.setValSecondaryTable( "valSecondaryTableFromSubA" );
			session.persist( subA );

			SubB subB = new SubB( 2L );
			subB.setValSubB( "valSubB" );
			subB.setValSecondaryTable( "valSecondaryTableFromSubB" );
			session.persist( subB );

			Query<String> query = session.createQuery( "select suba.valSecondaryTable from SubA suba", String.class );
			List<String> resultList = query.getResultList();
			Assert.assertEquals( 1, resultList.size() );
			Assert.assertEquals( "valSecondaryTableFromSubA", resultList.get( 0 ) );
		} );
	}

	@Test
	public void shouldNotReturnSecondaryTableValueForSubB_implicitJoin() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			SubA subA = new SubA( 1L );
			subA.setValSubA( "valSubA" );
			subA.setValSecondaryTable( "valSecondaryTableFromSubA" );
			session.persist( subA );

			SubB subB = new SubB( 2L );
			subB.setValSubB( "valSubB" );
			subB.setValSecondaryTable( "valSecondaryTableFromSubB" );
			session.persist( subB );

			C c = new C();
			c.setSubA( subA );
			session.persist( c );

			Query<String> query = session.createQuery( "select c.subA.valSecondaryTable from C c", String.class );
			List<String> resultList = query.getResultList();
			Assert.assertEquals( 1, resultList.size() );
			Assert.assertEquals( "valSecondaryTableFromSubA", resultList.get( 0 ) );
		} );
	}

	@Entity(name = "A")
	@Table(name = A.TABLE)
	@Inheritance(strategy = InheritanceType.JOINED)
	static class A {

		public static final String TABLE = "A_Table";

		public A() {
		}

		public A(Long id) {
			this.id = id;
		}

		@Id
		private Long id;

		private String valA;

		public Long getId() {
			return id;
		}

		public String getValA() {
			return valA;
		}

		public void setValA(String valA) {
			this.valA = valA;
		}

	}

	@Entity(name = "SubA")
	@Table(name = SubA.TABLE)
	@SecondaryTable(name = SECONDARY_TABLE_NAME, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	static class SubA extends A {

		public static final String TABLE = "SubA_table";

		public SubA() {
		}

		public SubA(Long id) {
			super( id );
		}

		private String valSubA;

		@Column(table = SECONDARY_TABLE_NAME)
		private String valSecondaryTable;

		public String getValSubA() {
			return valSubA;
		}

		public void setValSubA(String valSubA) {
			this.valSubA = valSubA;
		}

		public String getValSecondaryTable() {
			return valSecondaryTable;
		}

		public void setValSecondaryTable(String valSecondaryTable) {
			this.valSecondaryTable = valSecondaryTable;
		}
	}

	@Entity(name = "B")
	@Table(name = B.TABLE)
	@Inheritance(strategy = InheritanceType.JOINED)
	static class B {

		public static final String TABLE = "B_Table";

		public B() {
		}

		public B(Long id) {
			this.id = id;
		}

		@Id
		private Long id;

		private String valB;

		public Long getId() {
			return id;
		}

		public String getValB() {
			return valB;
		}

		public void setValB(String valB) {
			this.valB = valB;
		}

	}

	@Entity(name = "SubB")
	@Table(name = SubB.TABLE)
	@SecondaryTable(name = SECONDARY_TABLE_NAME, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	static class SubB extends B {

		public static final String TABLE = "SubB_table";

		public SubB() {
		}

		public SubB(Long id) {
			super( id );
		}

		private String valSubB;

		@Column(table = SECONDARY_TABLE_NAME)
		private String valSecondaryTable;

		public String getValSubB() {
			return valSubB;
		}

		public void setValSubB(String valSubB) {
			this.valSubB = valSubB;
		}

		public String getValSecondaryTable() {
			return valSecondaryTable;
		}

		public void setValSecondaryTable(String valSecondaryTable) {
			this.valSecondaryTable = valSecondaryTable;
		}
	}

	@Entity(name = "C")
	@Table(name = C.TABLE)
	static class C {

		public static final String TABLE = "C_table";

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private SubA subA;

		public Long getId() {
			return id;
		}

		public SubA getSubA() {
			return subA;
		}

		public void setSubA(SubA subA) {
			this.subA = subA;
		}
	}

}
