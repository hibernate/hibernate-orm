/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.basic;


import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.test.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.lazy.basic.LazyBasicFieldAccessTestTask.RecordData;
import org.junit.Assert;

/**
 * @author Gail Badner
 */
public class LazyBasicFieldAccessTestTask extends AbstractEnhancerTestTask {

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
		entity.setData( new RecordData( "eager.txt", "text/plain", "eager content".getBytes( StandardCharsets.UTF_8 ) ) );
		//entity.setLazyData( new RecordData( "lazy.txt", "text/plain", "lazy content".getBytes( StandardCharsets.UTF_8 ) ) );
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

		Assert.assertTrue( Hibernate.isPropertyInitialized( entity, "data" ) );
		RecordData data = entity.getData();
		Assert.assertNotNull(data);
		Assert.assertEquals( "eager.txt", data.getName() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( data, "content" ) );

		/*
		Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
		RecordData lazyData = entity.getLazyData();
		Assert.assertEquals( "lazy.txt", lazyData.getName() );
		Assert.assertFalse( Hibernate.isPropertyInitialized( lazyData, "content" ) );
		*/
		s.getTransaction().commit();
		s.close();

		s = getFactory().openSession();
		s.beginTransaction();
		entity.setDescription( "desc1" );
		s.update( entity );

		//Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
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

	@Embeddable
	public static class RecordData implements Serializable {
		private String name;

		private String mimeType;

		private byte[] content;

		public RecordData() {
		}

		public RecordData(String name, String mimeType, byte[] content) {
			this.name = name;
			this.mimeType = mimeType;
			this.content = content;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		@Basic(fetch = FetchType.LAZY)
		@Lob
		public byte[] getContent() {
			return content;
		}

		public void setContent(byte[] content) {
			this.content = content;
		}
	}

	@javax.persistence.Entity
	@Table(name = "lazy_field_access")
	public static class Entity {
		private Long id;

		private String description;

		private RecordData data;

		//private RecordData lazyData;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Basic(fetch = FetchType.LAZY)
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@Embedded
		public RecordData getData() {
			return data;
		}

		public void setData(RecordData data) {
			this.data = data;
		}
		/*
		@Embedded
		@Basic(fetch = FetchType.LAZY)
		public RecordData getLazyData() {
			return lazyData;
		}

		public void setLazyData(RecordData lazyData) {
			this.lazyData = lazyData;
		}
		*/
	}


}
