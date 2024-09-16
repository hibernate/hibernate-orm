/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@JiraKey( value = "HHH-9662" )
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
