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

import org.hibernate.metamodel.binding.EntityBinding;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class InheritanceTypeTest extends BaseAnnotationBindingTestCase {
	@Test
	public void testNoInheritance() {
		buildMetadataSources( SingleEntity.class );
		EntityBinding entityBinding = getEntityBinding( SingleEntity.class );
		assertNull( entityBinding.getEntityDiscriminator() );
	}

	@Test
	public void testDiscriminatorValue() {
		buildMetadataSources(
				RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class
		);
		EntityBinding entityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		assertEquals( "Wrong discriminator value", "foo", entityBinding.getDiscriminatorValue() );
	}

	@Test
	public void testRootEntityBinding() {
		buildMetadataSources(
				SubclassOfSingleTableInheritance.class, SingleEntity.class, RootOfSingleTableInheritance.class
		);

		EntityBinding noInheritanceEntityBinding = getEntityBinding( SingleEntity.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );

		assertTrue( noInheritanceEntityBinding.isRoot() );
		assertSame( noInheritanceEntityBinding, getRootEntityBinding( SingleEntity.class ) );

		assertFalse( subclassEntityBinding.isRoot() );
		assertSame( rootEntityBinding, getRootEntityBinding( SubclassOfSingleTableInheritance.class ) );

		assertTrue( rootEntityBinding.isRoot() );
		assertSame( rootEntityBinding, getRootEntityBinding( RootOfSingleTableInheritance.class ));
	}

	@Entity
	class SingleEntity {
		@Id
		@GeneratedValue
		private int id;
	}
}


