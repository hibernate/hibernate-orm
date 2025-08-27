/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Phone.class
		};
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails at SERIALIZABLE isolation")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Phone phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-7890");
			entityManager.persist(phone);

			return phone;
		});

		//tag::locking-optimistic-exclude-attribute-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Phone phone = entityManager.find(Phone.class, 1L);
			phone.setNumber("+123-456-7890");

			doInJPA(this::entityManagerFactory, _entityManager -> {
				Phone _phone = _entityManager.find(Phone.class, 1L);
				_phone.incrementCallCount();

				log.info("Bob changes the Phone call count");
			});

			log.info("Alice changes the Phone number");
		});
		//end::locking-optimistic-exclude-attribute-example[]
	}

	//tag::locking-optimistic-exclude-attribute-mapping-example[]
	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "`number`")
		private String number;

		@OptimisticLock(excluded = true)
		private long callCount;

		@Version
		private Long version;

		//Getters and setters are omitted for brevity

	//end::locking-optimistic-exclude-attribute-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Long getVersion() {
			return version;
		}

		public long getCallCount() {
			return callCount;
		}
	//tag::locking-optimistic-exclude-attribute-mapping-example[]
		public void incrementCallCount() {
			this.callCount++;
		}
	}
	//end::locking-optimistic-exclude-attribute-mapping-example[]
}
