/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@TestForIssue( jiraKey = "HHH-9662" )
public class CompositeIdIdentityTest extends BaseCoreFunctionalTestCase {

	@Test
	@FailureExpected( jiraKey = "HHH-9662" )
	public void testCompositePkWithIdentity() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Animal animal = new Animal();
			animal.setSubId( 123L );
			session.persist(animal);
		} );
	}


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			Animal.class
		};
	}

	@Entity
	@Table(name = "animal")
	@IdClass(IdWithSubId.class)
	public static class Animal {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Id
		@Column(name = "sub_id")
		private Long subId;

		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}

		public Long getSubId() {
			return subId;
		}
		public void setSubId(Long subId) {
			this.subId = subId;
		}
	}

	public static class IdWithSubId implements Serializable {
		private Long id;

		private Long subId;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getSubId() {
			return subId;
		}
		public void setSubId(Long subId) {
			this.subId = subId;
		}

	}
}
