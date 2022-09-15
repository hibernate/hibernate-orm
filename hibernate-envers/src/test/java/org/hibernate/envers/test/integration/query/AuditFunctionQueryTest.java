/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.query.sqm.produce.function.StandardFunctions;

import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class AuditFunctionQueryTest extends BaseEnversJPAFunctionalTestCase {

	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {

		@Id
		private Long id;
		private String string1;
		private String string2;
		private Integer integer1;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getString1() {
			return string1;
		}

		public void setString1(String string1) {
			this.string1 = string1;
		}

		public String getString2() {
			return string2;
		}

		public Integer getInteger1() {
			return integer1;
		}

		public void setInteger1(Integer integer1) {
			this.integer1 = integer1;
		}

		public void setString2(String string2) {
			this.string2 = string2;
		}

	}

	private TestEntity testEntity1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ TestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		testEntity1 = new TestEntity();
		testEntity1.setId( 1L );
		testEntity1.setString1( "abcdef" );
		testEntity1.setString2( "42 - the truth" );
		testEntity1.setInteger1(42 );
		em.persist( testEntity1 );
		em.getTransaction().commit();
	}

	@Test
	public void testProjectionWithPropertyArgument() {
		Object actual = getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.addProjection( AuditEntity.function( "upper", AuditEntity.property( "string1" ) ) ).getSingleResult();
		String expected = testEntity1.getString1().toUpperCase();
		assertEquals( "Expected the property string1 to be upper case", expected, actual );
	}

	@Test
	public void testProjectionWithPropertyAndSimpleArguments() {
		Object actual = getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.addProjection( AuditEntity.function( "substring", AuditEntity.property( "string1" ), 3, 2 ) ).getSingleResult();
		// the sql substring indices are 1 based while java substring indices are 0 based
		// the sql substring second parameter denotes the length of the substring
		// while in java the scond argument denotes the end index
		String expected = testEntity1.getString1().substring( 2, 4 );
		assertEquals( "Expected the substring of the property string1", expected, actual );
	}

	@Test
	public void testProjectionWithNestedFunction() {
		Object actual = getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.addProjection( AuditEntity.function( StandardFunctions.CONCAT,
						AuditEntity.function( "upper", AuditEntity.property( "string1" ) ),
						AuditEntity.function( "substring", AuditEntity.property( "string2" ), 1, 2 ) ) )
				.getSingleResult();
		final String expected = testEntity1.getString1().toUpperCase().concat( testEntity1.getString2().substring( 0, 2 ) );
		assertEquals( "Expected the upper cased string1 to be concat with the first two characters of string2", expected, actual );
	}

	@Test
	public void testComparisonFunctionWithValue() {
		TestEntity entity = (TestEntity) getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.add( AuditEntity.function( "substring", AuditEntity.property( "string1" ), 3, 2 ).eq( "cd" ) ).getSingleResult();
		assertNotNull( "Expected the entity to be returned", entity );
		assertEquals( "Expected the entity to be returned", testEntity1.getId(), entity.getId() );
	}

	@Test
	public void testComparionFunctionWithProperty() {
		TestEntity entity = (TestEntity) getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.add( AuditEntity.function( "length", AuditEntity.property( "string2" ) ).ltProperty( "integer1" ) ).getSingleResult();
		assertNotNull( "Expected the entity to be returned", entity );
		assertEquals( "Expected the entity to be returned", testEntity1.getId(), entity.getId() );
	}

	@Test
	public void testComparisonFunctionWithFunction() {
		TestEntity entity = (TestEntity) getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.add( AuditEntity.function( "substring", AuditEntity.property( "string2" ), 1, 2 )
						.eqFunction( AuditEntity.function( "str", AuditEntity.property( "integer1" ) ) ) )
				.getSingleResult();
		assertNotNull( "Expected the entity to be returned", entity );
		assertEquals( "Expected the entity to be returned", testEntity1.getId(), entity.getId() );
	}

	@Test
	public void testComparisonPropertyWithFunction() {
		TestEntity entity = (TestEntity) getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.add( AuditEntity.property( "integer1" ).gtFunction( AuditEntity.function( "length", AuditEntity.property( "string2" ) ) ) ).getSingleResult();
		assertNotNull( "Expected the entity to be returned", entity );
		assertEquals( "Expected the entity to be returned", testEntity1.getId(), entity.getId() );
	}

	@Test
	public void testFunctionOnIdProperty() {
		TestEntity entity = (TestEntity) getAuditReader().createQuery().forEntitiesAtRevision( TestEntity.class, 1 )
				.add( AuditEntity.function( "str", AuditEntity.id() ).like( "%1%" ) ).getSingleResult();
		assertNotNull( "Expected the entity to be returned", entity );
		assertEquals( "Expected the entity to be returned", testEntity1.getId(), entity.getId() );
	}

}
