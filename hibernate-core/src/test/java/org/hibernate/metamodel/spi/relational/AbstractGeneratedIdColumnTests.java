/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.relational;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Identity column tests of {@code hbm.xml} and annotation binding code
 *
 * @author Gail Badner
 */
public abstract class AbstractGeneratedIdColumnTests extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	protected ServiceRegistry basicServiceRegistry() {
		return serviceRegistry;
	}

	@Test
	public void testNativeId() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForNativeId( sources );
		// use old identifier generators so that strategy=GenerationType.AUTO maps to "native" ID generator
		sources.getMetadataBuilder().withNewIdentifierGeneratorsEnabled( false );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithNativeId.class.getName() );
		assertEquals(
				serviceRegistry.getService( JdbcServices.class ).getDialect().supportsIdentityColumns(),
				entityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).isIdentity()
		);
	}

	@Test
	public void testSequenceId() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSequenceId( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithSequenceId.class.getName() );
		assertEquals( false, entityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).isIdentity() );
	}

	@Test
	public void testIdentityId() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForIdentityId( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithIdentityId.class.getName() );
		assertEquals( true, entityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).isIdentity() );
	}

	@Test
	public void testAssignedId() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForAssignedId( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithAssignedId.class.getName() );
		assertEquals( false, entityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).isIdentity() );
	}

	public abstract void addSourcesForNativeId(MetadataSources sources);

	public abstract void addSourcesForSequenceId(MetadataSources sources);

	public abstract void addSourcesForIdentityId(MetadataSources sources);

	public abstract void addSourcesForAssignedId(MetadataSources sources);

	@Entity
	public static class EntityWithNativeId {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		private Long id;

		public EntityWithNativeId() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	public static class EntityWithSequenceId {
		@Id
		@GeneratedValue(strategy=GenerationType.SEQUENCE)
		private Long id;

		public EntityWithSequenceId() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	public static class EntityWithIdentityId {
		@Id
		@GeneratedValue(strategy=GenerationType.IDENTITY)
		private Long id;

		public EntityWithIdentityId() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	public static class EntityWithAssignedId {
		@Id
		private Long id;

		public EntityWithAssignedId() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
