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
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * @author Hardy Ferentschik
 */
public class InheritanceTypeTest extends BaseUnitTestCase {
	@Test
	public void testNoInheritance() {
		MetadataImpl meta = buildMetadata( SingleEntity.class );
		EntityBinding entityBinding = getEntityBindingForInnerClass( meta, SingleEntity.class );
		assertNull( entityBinding.getEntityDiscriminator() );
	}

	@Test
	public void testDiscriminatorValue() {
		MetadataImpl meta = buildMetadata( RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class );
		EntityBinding entityBinding = meta.getEntityBinding( SubclassOfSingleTableInheritance.class.getSimpleName() );
		assertEquals( "Wrong discriminator value", "foo", entityBinding.getDiscriminatorValue() );
	}

	private MetadataImpl buildMetadata(Class<?>... classes) {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		for ( Class clazz : classes ) {
			sources.addAnnotatedClass( clazz );
		}
		return (MetadataImpl) sources.buildMetadata();
	}

	private EntityBinding getEntityBindingForInnerClass(MetadataImpl meta, Class<?> clazz) {
		return meta.getEntityBinding( this.getClass().getSimpleName() + "$" + clazz.getSimpleName() );
	}

	@Entity
	class SingleEntity {
		@Id
		@GeneratedValue
		private int id;
	}
}


