/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinwithoutancestor;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue( jiraKey = "HHH-12993")
public class OmitAncestorJoinTest extends OmitAncestorTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { A.class, SubA.class, SubSubA.class, B.class };
	}

	@Test
	public void test() {
		// Should not join any parent table
		assertFromTables("select valSubA from SubA", SubA.TABLE);

		// Should not join any parent table
		assertFromTables("select sa.valSubA from SubA sa", SubA.TABLE);

		// Should omit middle table from inheritance hierarchy
		assertFromTables("select ssa.valA from SubSubA ssa", SubSubA.TABLE, A.TABLE);

		// Should omit middle table from inheritance hierarchy
		assertFromTables( "select ssa.valA, ssa.valSubSubA from SubSubA ssa", SubSubA.TABLE, A.TABLE );

		// Should join parent table, because it is used in "where" part
		assertFromTables("select valSubA from SubA where valA = 'foo'", SubA.TABLE, A.TABLE);

		// Should join parent table, because it is used in "order by" part
		assertFromTables("select valSubSubA from SubSubA order by valA", SubSubA.TABLE, A.TABLE);

		// Should other tables from hierarchye, because it returns whole entity
		assertFromTables("select suba from SubA suba", SubA.TABLE, A.TABLE, SubSubA.TABLE);
		assertFromTables("from SubA", SubA.TABLE, A.TABLE, SubSubA.TABLE);

		// Should join A table, because it has the reference to B table
		assertFromTables( "select suba.b from SubA suba", SubA.TABLE, A.TABLE, B.TABLE );
		assertFromTables( "select suba.b.id from SubA suba", SubA.TABLE, A.TABLE );
	}

	@Entity(name = "A")
	@Table(name = A.TABLE)
	@Inheritance(strategy = InheritanceType.JOINED)
	static class A {

		public static final String TABLE = "A_Table";

		@Id
		@GeneratedValue
		private Long id;

		private String valA;

		@ManyToOne
		private B b;

		public Long getId() {
			return id;
		}

		public String getValA() {
			return valA;
		}

		public void setValA(String valA) {
			this.valA = valA;
		}

		public B getB() {
			return b;
		}

		public void setB(B b) {
			this.b = b;
		}
	}

	@Entity(name = "SubA")
	@Table(name = SubA.TABLE)
	static class SubA extends A {

		public static final String TABLE = "SubA_Table";

		private String valSubA;

		public String getValSubA() {
			return valSubA;
		}

		public void setValSubA(String valSubA) {
			this.valSubA = valSubA;
		}
	}

	@Entity(name = "SubSubA")
	@Table(name = SubSubA.TABLE)
	static class SubSubA extends SubA {

		public static final String TABLE = "SubSubA_Table";

		private String valSubSubA;

		public String getValSubSubA() {
			return valSubSubA;
		}

		public void setValSubSubA(String valSubSubA) {
			this.valSubSubA = valSubSubA;
		}
	}

	@Entity(name = "B")
	@Table(name = B.TABLE)
	static class B {

		public static final String TABLE = "B_table";

		@Id
		@GeneratedValue
		private Long id;

		private String valB;

		@OneToMany(mappedBy = "b")
		private List<A> aList;

		public Long getId() {
			return id;
		}

		public String getValB() {
			return valB;
		}

		public void setValB(String valB) {
			this.valB = valB;
		}

		public List<A> getaList() {
			return aList;
		}

		public void setaList(List<A> aList) {
			this.aList = aList;
		}

	}

}
