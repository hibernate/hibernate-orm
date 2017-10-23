/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.basic;


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;

/**
 * @author Gail Badner
 */
public class LazyBasicPropertyAccessTestTask extends AbstractEnhancerTestTask {

	private Long entityId;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Entity.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		Entity entity = new Entity();
		entity.setDescription( "desc" );
		s.persist( entity );
		entityId = entity.getId();

		s.getTransaction().commit();
		s.clear();
		s.close();
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		Entity entity = s.get( Entity.class, entityId );

		Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
		EnhancerTestUtils.checkDirtyTracking( entity );

		Assert.assertEquals( "desc", entity.getDescription() );
		Assert.assertTrue( Hibernate.isPropertyInitialized( entity, "description" ) );

		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		entity.setDescription( "desc1" );
		s.update( entity );

		// Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
		EnhancerTestUtils.checkDirtyTracking( entity, "description" );

		Assert.assertEquals( "desc1", entity.getDescription() );
		Assert.assertTrue( Hibernate.isPropertyInitialized( entity, "description" ) );

		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		entity = s.get( Entity.class, entityId );
		Assert.assertEquals( "desc1", entity.getDescription() );
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		entity.setDescription( "desc2" );
		entity = (Entity) s.merge( entity );

		//Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
		EnhancerTestUtils.checkDirtyTracking( entity, "description" );

		Assert.assertEquals( "desc2", entity.getDescription() );
		Assert.assertTrue( Hibernate.isPropertyInitialized( entity, "description" ) );

		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		entity = s.get( Entity.class, entityId );
		Assert.assertEquals( "desc2", entity.getDescription() );
		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

	@javax.persistence.Entity
	@Access(AccessType.FIELD )
	@Table(name="lazy_property_access")
	public static class Entity {
		@Id
		@GeneratedValue
		private Long id;

		@Basic(fetch = FetchType.LAZY)
		private String description;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}


}
