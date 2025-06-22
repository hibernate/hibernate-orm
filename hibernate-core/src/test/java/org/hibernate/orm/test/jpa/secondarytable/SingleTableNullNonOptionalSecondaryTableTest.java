/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;

import org.hibernate.annotations.SecondaryRow;

import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
@RunWith(CustomParameterized.class)
public class SingleTableNullNonOptionalSecondaryTableTest extends AbstractNonOptionalSecondaryTableTest {

	public SingleTableNullNonOptionalSecondaryTableTest(JpaComplianceCachingSetting jpaComplianceCachingSetting) {
		super( jpaComplianceCachingSetting );
	}

	@Test
	public void testRowAddedForNullValue() {
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AnEntity anEntity = new AnEntity( 1 );
					entityManager.persist( anEntity );
				}
		);
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AnEntity anEntity = entityManager.find( AnEntity.class, 1 );
					assertNotNull( anEntity );
					assertNull( anEntity.aDetail );
					// assert that a row was inserted into Details when its property is null
					final Number id = (Number) entityManager.createNativeQuery(
							"select id from Details where aDetail is null"
					).getSingleResult();
					assertEquals( 1, id.intValue() );
				}
		);
	}

	@Test
	public void testRowAddedForNullValueInSubclassTable() {
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AnEntitySubclass anEntity = new AnEntitySubclass( 1 );
					entityManager.persist( anEntity );
				}
		);
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AnEntity anEntity = entityManager.find( AnEntity.class, 1 );
					assertNotNull( anEntity );
					assertNull( anEntity.aDetail );
					// assert that a row was inserted into Details when its property is null
					Number id = (Number) entityManager.createNativeQuery(
							"select id from Details where aDetail is null"
					).getSingleResult();
					assertEquals( 1, id.intValue() );
					// assert that a row was inserted into MoreDetails when its property is null
					id = (Number) entityManager.createNativeQuery(
							"select id from MoreDetails where anotherDetail is null"
					).getSingleResult();
					assertEquals( 1, id.intValue() );
				}
		);
	}

	@Test
	public void testEntityWithBadDataInBaseSecondaryTableIgnored() {
		// Not sure we really want to support this;
		// It only works with single-table inheritance.
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AnEntitySubclass anEntity = new AnEntitySubclass( 1 );
					entityManager.persist( anEntity );
				}
		);

		doInJPA(
				this::entityManagerFactory, entityManager -> {
					// Delete the data in a secondary table
					entityManager.createNativeQuery( "delete from Details where id = 1" ).executeUpdate();
					// The entity with bad data should be ignored.
					AnEntitySubclass anEntity = entityManager.find( AnEntitySubclass.class, 1 );
					assertNull( anEntity );
				}
		);
	}

	@Test
	public void testEntityWithBadDataInSubclassSecondaryTableIgnored() {
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					AnEntitySubclass anEntity = new AnEntitySubclass( 1 );
					entityManager.persist( anEntity );
				}
		);

		doInJPA(
				this::entityManagerFactory, entityManager -> {
					// Delete the data in a secondary table
					entityManager.createNativeQuery( "delete from MoreDetails where id = 1" ).executeUpdate();
					// The entity with bad data should be ignored.
					AnEntitySubclass anEntity = entityManager.find( AnEntitySubclass.class, 1 );
					assertNull( anEntity );
				}
		);
	}

	@After
	public void cleanupData() {
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					entityManager.createNativeQuery( "delete from Details" ).executeUpdate();
					entityManager.createNativeQuery( "delete from MoreDetails" ).executeUpdate();
					entityManager.createNativeQuery( "delete from AnEntity" ).executeUpdate();
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AnEntity.class, AnEntitySubclass.class };
	}

	@Entity(name = "AnEntity")
	@SecondaryTable(name = "Details")
	@SecondaryRow(table = "Details", optional = false)
	public static class AnEntity {
		@Id
		private int id;

		@Column(name = "aDetail", table="Details")
		private String aDetail;

		public AnEntity() {
		}

		public AnEntity(int id) {
			this.id = id;
		}
	}

	@Entity(name = "AnEntitySubclass")
	@SecondaryTable( name = "MoreDetails" )
	@SecondaryRow(table = "MoreDetails", optional = false)
	public static class AnEntitySubclass extends AnEntity {
		@Column(name = "anotherDetail", table="MoreDetails")
		private String anotherDetail;

		public AnEntitySubclass() {
		}

		public AnEntitySubclass(int id) {
			super( id );
		}
	}
}
