/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "12096")
public class GetterAndIsMethodChecks extends BaseUnitTestCase {

	@Test
	public void testIt() {
		new MetadataSources().addAnnotatedClass( A.class ).buildMetadata().buildSessionFactory().close();
	}

	@Entity( name= "A" )
	public static class A {
		@Id
		private Integer id;
		@OneToOne
		private A b;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public boolean isB() {
			return true;
		}

		public A getB() {
			return b;
		}

		public void setB(A b) {
			this.b = b;
		}
	}
}
