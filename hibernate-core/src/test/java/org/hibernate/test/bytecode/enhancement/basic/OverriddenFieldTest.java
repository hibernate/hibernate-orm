package org.hibernate.test.bytecode.enhancement.basic;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

/**
 * Tests persisting and then loading a property with bytecode enhancement enabled
 * when the entity has the same field defined twice: once in a mappedsuperclass (should be ignored)
 * and once in the concrete entity class.
 */
@TestForIssue(jiraKey = "HHH-15505")
@RunWith(BytecodeEnhancerRunner.class)
public class OverriddenFieldTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AbstractEntity.class, Fruit.class };
	}

	@Before
	public void prepare() {
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, s -> {
			Fruit testEntity = new Fruit();
			testEntity.setId( 1 );
			testEntity.setName( "John" );
			s.persist( testEntity );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Fruit testEntity = s.get( Fruit.class, 1 );
			Assert.assertEquals( "John", testEntity.getName() );
		} );
	}

	@MappedSuperclass
	public static class AbstractEntity {

		@Column(length = 40, unique = true)
		private String name;

	}

	@Entity
	@Table(name = "known_fruits")
	public static class Fruit extends AbstractEntity {

		@Id
		private Integer id;

		@Column(length = 40, unique = true)
		private String name;

		public Fruit() {
		}

		public Fruit(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
