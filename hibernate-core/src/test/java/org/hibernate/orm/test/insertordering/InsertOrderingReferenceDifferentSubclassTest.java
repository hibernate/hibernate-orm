/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Normunds Gavars
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14227")
public class InsertOrderingReferenceDifferentSubclassTest extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				SubclassA.class,
				SubclassB.class
		};
	}

	@Test
	public void testReferenceDifferentSubclass() {
		sessionFactory().inTransaction( session -> {
			SubclassA subclassA1 = new SubclassA();
			SubclassB subclassB1 = new SubclassB();

			SubclassA subclassA2 = new SubclassA();
			SubclassB subclassB2 = new SubclassB();

			subclassA1.referenceB = subclassB2;
			subclassB2.referenceA = subclassA2;

			subclassA2.referenceB = subclassB1;

			session.persist( subclassA1 );
			session.persist( subclassA2 );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into SubclassB (name,referenceA_id,id) values (?,?,?)", 2 ),
				new Batch( "insert into SubclassA (name,referenceB_id,id) values (?,?,?)", 2 )
		);
	}

	@MappedSuperclass
	static class BaseClass {

		@Id
		@GeneratedValue
		Long id;

		String name;

	}

	@Entity(name = "SubclassA")
	static class SubclassA extends BaseClass {

		@OneToOne(cascade = CascadeType.ALL)
		SubclassB referenceB;

	}

	@Entity(name = "SubclassB")
	static class SubclassB extends BaseClass {

		@ManyToOne(fetch = FetchType.LAZY)
		SubclassA referenceA;

	}
}
