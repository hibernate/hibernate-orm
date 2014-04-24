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
package org.hibernate.metamodel.spi.binding;

import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * "Unsaved" value tests of {@code hbm.xml} and annotation binding code
 *
 * @author Gail Badner
 */
public abstract class AbstractUnsavedValueTests extends BaseUnitTestCase {
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
	public void testAssignedSimpleIdDefaultUnsavedValue() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForAssignedIdDefaultUnsavedValue( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithAssignedId.class.getName() );
		// Generator is explicitly "assigned", so unsaved ID value should be "undefined"
		assertEquals( "undefined", entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getUnsavedValue() );
	}

	@Test
	public void testIncrementSimpleIdDefaultUnsavedValue() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSequenceIdDefaultUnsavedValue( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithSequenceId.class.getName() );
		// Generator is explicitly "increment", so unsaved ID value should be null
		assertEquals( null, entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getUnsavedValue() );
	}

	@Test
	public void testDefaultUnsavedVersion() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForDefaultUnsavedVersion( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithVersion.class.getName() );
		// Generator is explicitly "assigned", so unsaved ID value should be "undefined"
		assertEquals( "undefined", entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getUnsavedValue() );
	}

	@Test
	public void testDefaultUnsavedTimestamp() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForDefaultUnsavedTimestamp( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithTimestamp.class.getName() );
		// Generator is explicitly "assigned", so unsaved ID value should be "undefined"
		assertEquals( "undefined", entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getUnsavedValue() );
	}

	public abstract void addSourcesForAssignedIdDefaultUnsavedValue(MetadataSources sources);

	public abstract void addSourcesForSequenceIdDefaultUnsavedValue(MetadataSources sources);

	public abstract void addSourcesForDefaultUnsavedVersion(MetadataSources sources);

	public abstract void addSourcesForDefaultUnsavedTimestamp(MetadataSources sources);

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

	@Entity
	public static class EntityWithSequenceId {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
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
	public static class EntityWithVersion {
		@Id
		private Long id;

		@Version
		private Integer version;

		public EntityWithVersion() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}
	}

	@Entity
	public static class EntityWithTimestamp {
		@Id
		private Long id;

		@Version
		@Temporal( value = TemporalType.TIMESTAMP )
		private Timestamp timestamp;

		public EntityWithTimestamp() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Timestamp getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Timestamp timestamp) {
			this.timestamp = timestamp;
		}
	}
}
