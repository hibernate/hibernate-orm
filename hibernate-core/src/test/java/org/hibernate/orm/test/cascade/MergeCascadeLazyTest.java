/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(
		annotatedClasses = {
				MergeCascadeLazyTest.MasterEntity.class,
				MergeCascadeLazyTest.ReferencedEntity.class,
				MergeCascadeLazyTest.LazyEntity.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-6718")
public class MergeCascadeLazyTest {

	@Test
	void test(SessionFactoryScope scope) {
		MasterEntity masterEntity = scope.fromTransaction(
				session -> {
					ReferencedEntity re = new ReferencedEntity( 1L );
					session.persist( re );
					MasterEntity me = new MasterEntity( re );
					session.persist( me );
					return me;
				}
		);
		scope.inTransaction(
				session -> {
					masterEntity.data = "modified";
					MasterEntity mergedEntity = session.merge( masterEntity );
					// See the discussion on the Jira issue. One of the two assertion groups should hold
					assertFalse( Hibernate.isInitialized( mergedEntity.reference ) );

//					assertTrue( Hibernate.isInitialized( mergedEntity.reference ) );
//					assertFalse( Hibernate.isInitialized( mergedEntity.reference.lazyReference ) );
				}
		);
	}

	@Entity
	public static class LazyEntity {
		@Id
		public Long id;
		@Version
		public Long concurrency;

		public LazyEntity() {
		}

		public LazyEntity(Long id) {
			this.id = id;
		}
	}

	@Entity
	public static class MasterEntity {
		@Id
		public Long id;
		@Version
		public Long concurrency;
		public String data;
		@OneToOne
		@Fetch(FetchMode.SELECT)
		public ReferencedEntity reference;

		protected MasterEntity() {
		}

		public MasterEntity(ReferencedEntity reference) {
			this.id = reference.id;
			this.reference = reference;
			this.data = "initial";
		}
	}

	@Entity
	public static class ReferencedEntity {
		@Id
		public Long id;
		@Version
		public Long concurrency;
		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
		public LazyEntity lazyReference;

		public ReferencedEntity() {

		}
		public ReferencedEntity(Long id) {
			this.id = id;
			this.lazyReference = new LazyEntity(id);
		}
	}


}
