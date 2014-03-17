/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.annotations.Source;
import org.hibernate.annotations.SourceType;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.LongType;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class VersionBindingTests extends BaseAnnotationBindingTestCase {
	@Entity
	class Item1 {
		@Id
		long id;
		long version;
	}

	@Test
	@Resources(annotatedClasses = VersionBindingTests.Item1.class)
	public void testNoVersionAnnotation() {
		assertFalse( getEntityBinding( Item1.class ).getHierarchyDetails().isVersioned() );
	}

	@Entity
	class Item2 {
		@Id
		private long id;
		@Version
		private Long version;

		//we need add getters / setters due to HHH-6561
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

	}

	@Test
	@Resources(annotatedClasses = VersionBindingTests.Item2.class)
	public void testVersionTypeAttribute() {
		EntityBinding binding = getEntityBinding( Item2.class );
		assertTrue( binding.getHierarchyDetails().isVersioned() );
		HibernateTypeDescriptor descriptor = binding.getHierarchyDetails()
				.getEntityVersion()
				.getVersioningAttributeBinding()
				.getHibernateTypeDescriptor();
//		assertEquals( "Long", descriptor.getExplicitTypeName() );
		assertEquals(				Long.class.getName(),
				descriptor.getJavaTypeDescriptor().getName().toString()
		);
		assertNotNull( descriptor.getResolvedTypeMapping() );
		assertEquals( LongType.class, descriptor.getResolvedTypeMapping().getClass() );
		assertNotNull( descriptor.getTypeParameters() );
		assertTrue( descriptor.getTypeParameters().isEmpty() );
	}

	@Test
	@Resources(annotatedClasses = VersionBindingTests.Item2.class)
	public void testVersionUnsavedValue() {
		EntityBinding binding = getEntityBinding( Item2.class );
		assertTrue( binding.getHierarchyDetails().isVersioned() );
		assertEquals( "undefined", binding.getHierarchyDetails().getEntityVersion().getUnsavedValue() );
	}
	
	@Entity
	class Item3 {
		@Id
		Long id;
		@Version
		@Source(SourceType.DB)
		Date version;
	}

	@Test
	@Resources(annotatedClasses = VersionBindingTests.Item3.class)
	public void testVersionAttributeWithSource() {
		EntityBinding binding = getEntityBinding( Item3.class );
		assertTrue( binding.getHierarchyDetails().isVersioned() );
		HibernateTypeDescriptor descriptor = binding.getHierarchyDetails()
				.getEntityVersion()
				.getVersioningAttributeBinding()
				.getHibernateTypeDescriptor();
		assertEquals( "dbtimestamp", descriptor.getExplicitTypeName() );
		assertEquals(
				Date.class.getName(),
				descriptor.getJavaTypeDescriptor().getName().toString()
		);
		assertNotNull( descriptor.getResolvedTypeMapping() );
		assertEquals( DbTimestampType.class, descriptor.getResolvedTypeMapping().getClass() );
		assertNotNull( descriptor.getTypeParameters() );
		assertTrue( descriptor.getTypeParameters().isEmpty() );
	}
}
