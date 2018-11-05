/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinwithoutancestor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-12993")
public class OmitAncestorJoinWhenSecondaryTablePresentTest extends OmitAncestorTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { A.class, SubA.class, SubSubA.class };
	}

	@Test
	public void test() {
		assertFromTables( "select valSubASecondaryTable from SubA", SubA.TABLE, SubSubA.TABLE, SubA.SECONDARY_TABLE );
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
	@SecondaryTable(name = SubA.SECONDARY_TABLE)
	static class SubA extends A {

		public static final String TABLE = "SubA_Table";
		public static final String SECONDARY_TABLE = "SubA_Table_Sec";

		private String valSubA;

		@Column(table = SECONDARY_TABLE)
		private String valSubASecondaryTable;

		public String getValSubA() {
			return valSubA;
		}

		public void setValSubA(String valSubA) {
			this.valSubA = valSubA;
		}

		public String getValSubASecondaryTable() {
			return valSubASecondaryTable;
		}

		public void setValSubASecondaryTable(String valSubASecondaryTable) {
			this.valSubASecondaryTable = valSubASecondaryTable;
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

}
