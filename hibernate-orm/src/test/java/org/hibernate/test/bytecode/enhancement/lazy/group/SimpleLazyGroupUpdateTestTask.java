/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class SimpleLazyGroupUpdateTestTask extends AbstractEnhancerTestTask {
	public static final String REALLY_BIG_STRING = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Override
	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		s.save( new TestEntity( 1, "entity 1", "blah", REALLY_BIG_STRING ) );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		TestEntity entity = s.load( TestEntity.class, 1 );
		assertLoaded( entity, "name" );
		assertNotLoaded( entity, "lifeStory" );
		assertNotLoaded( entity, "reallyBigString" );

		entity.setLifeStory( "blah blah blah" );
		assertLoaded( entity, "name" );
		assertLoaded( entity, "lifeStory" );
		assertNotLoaded( entity, "reallyBigString" );

		s.getTransaction().commit();
		s.close();


		s = getFactory().openSession();
		s.beginTransaction();

		entity = s.load( TestEntity.class, 1 );
		assertLoaded( entity, "name" );
		assertNotLoaded( entity, "lifeStory" );
		assertNotLoaded( entity, "reallyBigString" );
		assertEquals( "blah blah blah", entity.getLifeStory() );
		assertEquals( REALLY_BIG_STRING, entity.getReallyBigString() );

		s.getTransaction().commit();
		s.close();
	}

	private void assertLoaded(Object owner, String name) {
		// NOTE we assume null == not-loaded
		Object fieldByReflection = EnhancerTestUtils.getFieldByReflection( owner, name );
		assertNotNull( "Expecting field '" + name + "' to be loaded, but it was not", fieldByReflection );
	}

	private void assertNotLoaded(Object owner, String name) {
		// NOTE we assume null == not-loaded
		Object fieldByReflection = EnhancerTestUtils.getFieldByReflection( owner, name );
		assertNull( "Expecting field '" + name + "' to be not loaded, but it was", fieldByReflection );
	}

	@Override
	protected void cleanup() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		s.createQuery( "delete TestEntity" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		Integer id;
		String name;
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup( "grp1" )
		String lifeStory;
		@Basic(fetch = FetchType.LAZY)
		@LazyGroup( "grp2" )
		String reallyBigString;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name, String lifeStory, String reallyBigString) {
			this.id = id;
			this.name = name;
			this.lifeStory = lifeStory;
			this.reallyBigString = reallyBigString;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLifeStory() {
			return lifeStory;
		}

		public void setLifeStory(String lifeStory) {
			this.lifeStory = lifeStory;
		}

		public String getReallyBigString() {
			return reallyBigString;
		}

		public void setReallyBigString(String reallyBigString) {
			this.reallyBigString = reallyBigString;
		}
	}
}
