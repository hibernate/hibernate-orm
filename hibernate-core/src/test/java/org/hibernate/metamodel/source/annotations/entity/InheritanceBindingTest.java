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

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class InheritanceBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = SingleEntity.class)
	public void testNoInheritance() {
		EntityBinding entityBinding = getEntityBinding( SingleEntity.class );
		assertNull( entityBinding.getHierarchyDetails().getEntityDiscriminator() );
	}

	@Test
	@Resources(annotatedClasses = { RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class })
	public void testDiscriminatorValue() {
		EntityBinding entityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		assertEquals( "Wrong discriminator value", "foo", entityBinding.getDiscriminatorMatchValue() );
	}

	@Test
	@Resources(annotatedClasses = { RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class })
	public void testSubclassEntitySuperType() {
		EntityBinding entityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		assertNotNull( entityBinding.getEntity().getSuperType() );
		assertSame( RootOfSingleTableInheritance.class, entityBinding.getEntity().getSuperType().getClassReference() );
		assertEquals( RootOfSingleTableInheritance.class.getName(), entityBinding.getEntity().getSuperType().getClassName() );
		assertNull( entityBinding.getEntity().getSuperType().getSuperType() );
	}

	@Test
	@Resources(annotatedClasses = { RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class })
	public void testRootEntitySuperType() {
		EntityBinding entityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		assertNull( entityBinding.getEntity().getSuperType() );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class
	})
	public void testRootEntityBinding() {
		EntityBinding noInheritanceEntityBinding = getEntityBinding( SingleEntity.class );
		assertTrue( "SingleEntity should be a root entity", noInheritanceEntityBinding.isRoot() );
		assertSame( noInheritanceEntityBinding, getRootEntityBinding( SingleEntity.class ) );

		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		assertFalse( subclassEntityBinding.isRoot() );
		assertSame( rootEntityBinding, getRootEntityBinding( SubclassOfSingleTableInheritance.class ) );

		assertTrue( rootEntityBinding.isRoot() );
		assertSame( rootEntityBinding, getRootEntityBinding( RootOfSingleTableInheritance.class ) );
	}

	@Test
	@Resources(annotatedClasses = { RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class })
	public void testDefaultDiscriminatorOptions() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityDiscriminator discriminator = rootEntityBinding.getHierarchyDetails().getEntityDiscriminator();
		assertFalse( "Wrong default value", discriminator.isForced() );
		assertTrue( "Wrong default value", discriminator.isInserted() );
	}

	@Test
	@Resources(annotatedClasses = { Base.class, Jump.class })
	public void testExplicitDiscriminatorOptions() {
		EntityBinding rootEntityBinding = getEntityBinding( Base.class );
		EntityDiscriminator discriminator = rootEntityBinding.getHierarchyDetails().getEntityDiscriminator();
		assertTrue( "Wrong default value", discriminator.isForced() );
		assertFalse( "Wrong default value", discriminator.isInserted() );
	}

	@Entity
	class SingleEntity {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	class RootOfSingleTableInheritance {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	@DiscriminatorValue("foo")
	public class SubclassOfSingleTableInheritance extends RootOfSingleTableInheritance {
	}

	@Entity
	@DiscriminatorOptions(force = true, insert = false)
	class Base {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	class Jump extends Base {
	}
}


