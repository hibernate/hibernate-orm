/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.secondarytable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SecondaryTable;

import org.hibernate.annotations.Table;

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
public class JoinedTableNullNonOptionalSecondaryTableTest extends AbstractNonOptionalSecondaryTableTest {

	public JoinedTableNullNonOptionalSecondaryTableTest(JpaComplianceCachingSetting jpaComplianceCachingSetting) {
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
					assertEquals(
							1,
							id.intValue()
					);
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
					assertEquals( 1,id.intValue() );
				}
		);
	}

	@After
	public void cleanupData() {
		doInJPA(
				this::entityManagerFactory, entityManager -> {
					entityManager.createNativeQuery( "delete from Details" ).executeUpdate();
					entityManager.createNativeQuery( "delete from MoreDetails" ).executeUpdate();
					entityManager.createNativeQuery( "delete from AnEntitySubclass" ).executeUpdate();
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
	@Table(appliesTo = "Details", optional = false)
	@Inheritance(strategy = InheritanceType.JOINED)
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
	@Table(appliesTo = "MoreDetails", optional = false)
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