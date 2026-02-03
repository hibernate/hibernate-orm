/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/// Corollary to [OptimisticLockTest] using JPA's [ExcludedFromVersioning] instead of
/// Hibernate's [org.hibernate.annotations.OptimisticLock].
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = ExcludedFromVersioningTests.Phone.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
public class ExcludedFromVersioningTests {
	private static final Logger log = Logger.getLogger( ExcludedFromVersioningTests.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Fails at SERIALIZABLE isolation")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			var phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-7890");
			session.persist(phone);
		});

		factoryScope.inTransaction( session -> {
			var phone = session.find( Phone.class, 1L);
			phone.setNumber("+123-456-7890");

			factoryScope.inTransaction( _session -> {
				var _phone = _session.find( Phone.class, 1L);
				_phone.incrementCallCount();

				log.info("Bob changes the Phone call count");
			});

			log.info("Alice changes the Phone number");
		} );
		//end::locking-optimistic-exclude-attribute-example[]
	}

	@Entity(name = "Phone")
	public static class Phone {
		@Id
		private Long id;
		@Column(name = "`number`")
		private String number;

		@ExcludedFromVersioning
		private long callCount;

		@Version
		private Long version;

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

		public void incrementCallCount() {
			this.callCount++;
		}
	}
}
