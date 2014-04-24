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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.Value;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

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
		assertFalse( entityBinding.isPolymorphic() );
	}

	@Test
	@Resources(annotatedClasses = { RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class })
	public void testDiscriminatorValue() {
		EntityBinding entityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		assertEquals( "Wrong discriminator value", "foo1", entityBinding.getDiscriminatorMatchValue() );
	}

	@Test
	@Resources(annotatedClasses = { RootOfSingleTableInheritance.class, SubclassOfSingleTableInheritance.class })
	public void testSubclassEntitySuperType() {
		EntityBinding entityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		assertNotNull( entityBinding.getEntity().getSuperType() );
		assertEquals(
				RootOfSingleTableInheritance.class.getName(),
				entityBinding.getEntity().getSuperType().getDescriptor().getName().toString()
		);
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
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testNoPolymorphism() {
		EntityBinding noInheritanceEntityBinding = getEntityBinding( SingleEntity.class );
		assertTrue( "SingleEntity should be a root entity", noInheritanceEntityBinding.isRoot() );
		assertNull( noInheritanceEntityBinding.getSuperEntityBinding() );
		assertSame( noInheritanceEntityBinding, getRootEntityBinding( SingleEntity.class ) );
		assertFalse( noInheritanceEntityBinding.isPolymorphic() );
		assertFalse( noInheritanceEntityBinding.hasSubEntityBindings() );
		assertEquals( 0, noInheritanceEntityBinding.getSubEntityBindingClosureSpan() );
		assertEquals( 0, noInheritanceEntityBinding.getPostOrderSubEntityBindingClosure().length );
		assertEquals( 0, noInheritanceEntityBinding.getPreOrderSubEntityBindingClosure().length );
		Set<AttributeBinding> directAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : noInheritanceEntityBinding.attributeBindings() ) {
			assertTrue( directAttributeBindings.add( attributeBinding ) );
		}
		assertEquals( 1, directAttributeBindings.size() );
		assertSame(
				noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				directAttributeBindings.iterator().next()
		);
		assertEquals( 1, noInheritanceEntityBinding.getAttributeBindingClosureSpan() );
		Iterator<AttributeBinding> iterator = noInheritanceEntityBinding.attributeBindings().iterator();
		assertTrue( iterator.hasNext() );
		assertSame(
				noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				iterator.next()
		);
		assertFalse( iterator.hasNext() );

		AttributeBinding[] attributeBindings = noInheritanceEntityBinding.getAttributeBindingClosure();
		assertTrue( attributeBindings.length > 0 );
		int index =0;
		assertSame( noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(), attributeBindings[index++] );
		assertFalse( index < attributeBindings.length );
		attributeBindings =  noInheritanceEntityBinding.getEntitiesAttributeBindingClosure();
		index = 0;
		assertTrue( attributeBindings.length > 0 );
		assertSame( noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(), attributeBindings[index++] );
		assertFalse( index < attributeBindings.length );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testRootPolymporphism() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );

		assertTrue( rootEntityBinding.isRoot() );
		assertEquals( StringHelper.unqualify( rootEntityBinding.getEntityName() ), rootEntityBinding.getDiscriminatorMatchValue() );
		assertNull( rootEntityBinding.getSuperEntityBinding() );
		assertSame( rootEntityBinding, getRootEntityBinding( RootOfSingleTableInheritance.class ) );
		assertTrue( rootEntityBinding.isPolymorphic() );
		assertTrue( rootEntityBinding.hasSubEntityBindings() );
		Iterator<EntityBinding> directEntityBindingIterator = rootEntityBinding.getDirectSubEntityBindings().iterator();
		assertTrue( directEntityBindingIterator.hasNext() );
		EntityBinding directSubEntityBinding1 = directEntityBindingIterator.next();
		assertTrue( directEntityBindingIterator.hasNext() );
		EntityBinding directSubEntityBinding2 = directEntityBindingIterator.next();
		assertFalse( directEntityBindingIterator.hasNext() );
		boolean isSubclassEntityBindingFirst =  directSubEntityBinding1 == subclassEntityBinding;
		if ( isSubclassEntityBindingFirst ) {
			assertSame( otherSubclassEntityBinding, directSubEntityBinding2 );
		}
		else {
			assertSame( otherSubclassEntityBinding, directSubEntityBinding1 );
			assertSame( subclassEntityBinding, directSubEntityBinding2 );
		}
		Set<AttributeBinding> directAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : rootEntityBinding.attributeBindings() ) {
			assertTrue( directAttributeBindings.add( attributeBinding ) );
		}
		assertEquals( 1, directAttributeBindings.size() );
		assertTrue( directAttributeBindings.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertEquals( 1, rootEntityBinding.getAttributeBindingClosureSpan() );
		Set<AttributeBinding> attributeBindingClosure = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : rootEntityBinding.getAttributeBindingClosure() ) {
			assertTrue( attributeBindingClosure.add( attributeBinding ) );
		}
		assertEquals( 1, attributeBindingClosure.size() );
		assertTrue( attributeBindingClosure.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		Set<AttributeBinding> subAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding subAttributeBinding : rootEntityBinding.getEntitiesAttributeBindingClosure() ) {
			assertTrue( subAttributeBindings.add( subAttributeBinding ) );
		}
		assertEquals( 4, subAttributeBindings.size() );
		assertTrue( subAttributeBindings.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( subAttributeBindings.contains( subclassEntityBinding.locateAttributeBinding( "name" ) ) );
		assertTrue( subAttributeBindings.contains( subclassOfSubclassEntityBinding.locateAttributeBinding( "otherOtherName" ) ) );
		assertTrue( subAttributeBindings.contains( otherSubclassEntityBinding.locateAttributeBinding( "otherName" ) ) );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testPreOrderRootSubEntityClosure() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );
		// need to figure out the order of direct subclasses, since it's indeterminate
		Iterator<EntityBinding> directEntityBindingIterator = rootEntityBinding.getDirectSubEntityBindings().iterator();
		boolean isSubclassEntityBindingFirst = subclassEntityBinding == directEntityBindingIterator.next();
		assertEquals( 3, rootEntityBinding.getSubEntityBindingClosureSpan() );
		EntityBinding[] subEntityBindingIterator = rootEntityBinding.getPreOrderSubEntityBindingClosure();
		assertTrue( subEntityBindingIterator.length > 0 );
		int i = 0;
		if ( isSubclassEntityBindingFirst ) {
			assertSame( subclassEntityBinding, subEntityBindingIterator[i++] );
			assertTrue( i<subEntityBindingIterator.length );
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator[i++] );
			assertTrue( i<subEntityBindingIterator.length );
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator[i++] );
		}
		else {
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator[i++] );
			assertTrue( i<subEntityBindingIterator.length );
			assertSame( subclassEntityBinding, subEntityBindingIterator[i++] );
			assertTrue( i<subEntityBindingIterator.length );
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator[i++] );
		}
		assertFalse( i<subEntityBindingIterator.length );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testPostOrderRootSubEntityClosure() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );
		// need to figure out the order of direct subclasses, since it's indeterminate
		Iterator<EntityBinding> directEntityBindingIterator = rootEntityBinding.getDirectSubEntityBindings().iterator();
		boolean isSubclassEntityBindingFirst = subclassEntityBinding == directEntityBindingIterator.next();
		assertEquals( 3, rootEntityBinding.getSubEntityBindingClosureSpan() );
		EntityBinding[] subEntityBindingIterator = rootEntityBinding.getPostOrderSubEntityBindingClosure();
		int i =0;
		assertTrue( subEntityBindingIterator.length > 0 );
		if ( isSubclassEntityBindingFirst ) {
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator[i++] );
			assertSame( subclassEntityBinding, subEntityBindingIterator[i++] );
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator[i++] );
		}
		else {
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator[i++] );
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator[i++] );
			assertSame( subclassEntityBinding, subEntityBindingIterator[i++] );
		}
		assertFalse( i < subEntityBindingIterator.length );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testLeafSubclassOfRoot() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );

		assertEquals( "Wrong discriminator value", "foo2", otherSubclassEntityBinding.getDiscriminatorMatchValue() );
		assertFalse( otherSubclassEntityBinding.isRoot() );
		assertSame( rootEntityBinding, otherSubclassEntityBinding.getSuperEntityBinding() );
		assertSame( rootEntityBinding, getRootEntityBinding( OtherSubclassOfSingleTableInheritance.class) );
		assertTrue( otherSubclassEntityBinding.isPolymorphic() );
		assertFalse( otherSubclassEntityBinding.hasSubEntityBindings() );
		assertEquals( 0, otherSubclassEntityBinding.getSubEntityBindingClosureSpan() );
		assertFalse( otherSubclassEntityBinding.getPostOrderSubEntityBindingClosure().length > 0 );
		assertFalse( otherSubclassEntityBinding.getPreOrderSubEntityBindingClosure().length > 0 );
		Set<AttributeBinding> directAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : otherSubclassEntityBinding.attributeBindings() ) {
			assertTrue( directAttributeBindings.add( attributeBinding ) );
		}
		assertEquals( 1, directAttributeBindings.size() );
		assertTrue( directAttributeBindings.contains( otherSubclassEntityBinding.locateAttributeBinding( "otherName" ) ) );
		assertEquals( 2, otherSubclassEntityBinding.getAttributeBindingClosureSpan() );
		Set<AttributeBinding> attributeBindingClosure = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : otherSubclassEntityBinding.getAttributeBindingClosure() ) {
			assertTrue( attributeBindingClosure.add( attributeBinding ) );
		}
		assertEquals(2, attributeBindingClosure.size() );
		assertTrue( attributeBindingClosure.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( attributeBindingClosure.contains( otherSubclassEntityBinding.locateAttributeBinding( "otherName" ) ) );
		Set<AttributeBinding> subAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding subAttributeBinding : otherSubclassEntityBinding.getEntitiesAttributeBindingClosure() ) {
			assertTrue( subAttributeBindings.add( subAttributeBinding ) );
		}
		assertEquals( 2, subAttributeBindings.size() );
		assertTrue( subAttributeBindings.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( subAttributeBindings.contains( otherSubclassEntityBinding.locateAttributeBinding( "otherName" ) ) );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testNonLeafSubclassOfRootPolymporhism() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );

		assertEquals( "Wrong discriminator value", "foo1", subclassEntityBinding.getDiscriminatorMatchValue() );
		assertFalse( subclassEntityBinding.isRoot() );
		assertSame( rootEntityBinding, subclassEntityBinding.getSuperEntityBinding() );
		assertSame( rootEntityBinding, getRootEntityBinding( SubclassOfSingleTableInheritance.class ) );
		assertTrue( subclassEntityBinding.isPolymorphic() );
		assertTrue( subclassEntityBinding.hasSubEntityBindings() );
		assertEquals( 1, subclassEntityBinding.getSubEntityBindingClosureSpan() );
		EntityBinding[] itSubEntityBindings = subclassEntityBinding.getPostOrderSubEntityBindingClosure();
		int i = 0;
		assertTrue( i < itSubEntityBindings.length );
		assertSame( subclassOfSubclassEntityBinding, itSubEntityBindings[i++] );
		assertFalse( i < itSubEntityBindings.length );
		itSubEntityBindings = subclassEntityBinding.getPreOrderSubEntityBindingClosure();
		i = 0;
		assertTrue( i < itSubEntityBindings.length );
		assertSame( subclassOfSubclassEntityBinding, itSubEntityBindings[i++] );
		assertFalse( i < itSubEntityBindings.length );
		Set<AttributeBinding> directAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : subclassEntityBinding.attributeBindings() ) {
			assertTrue( directAttributeBindings.add( attributeBinding ) );
		}
		assertEquals( 1, directAttributeBindings.size() );
		assertTrue( directAttributeBindings.contains( subclassEntityBinding.locateAttributeBinding( "name" ) ) );
		assertEquals( 2, subclassEntityBinding.getAttributeBindingClosureSpan() );
		Set<AttributeBinding> attributeBindingClosure = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : subclassEntityBinding.getAttributeBindingClosure() ) {
			assertTrue( attributeBindingClosure.add( attributeBinding ) );
		}
		assertEquals( 2, attributeBindingClosure.size() );
		assertTrue( attributeBindingClosure.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( attributeBindingClosure.contains( subclassEntityBinding.locateAttributeBinding( "name" ) ) );
		Set<AttributeBinding> subAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding subAttributeBinding : subclassEntityBinding.getEntitiesAttributeBindingClosure() ) {
			assertTrue( subAttributeBindings.add( subAttributeBinding ) );
		}
		assertEquals( 3, subAttributeBindings.size() );
		assertTrue( subAttributeBindings.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( subAttributeBindings.contains( subclassEntityBinding.locateAttributeBinding( "name" ) ) );
		assertTrue( subAttributeBindings.contains( subclassOfSubclassEntityBinding.locateAttributeBinding( "otherOtherName" ) ) );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testLeafSubclassOfSubclassOfRootPolymporhism() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );

		assertEquals( "Wrong discriminator value", "foo1_1", subclassOfSubclassEntityBinding.getDiscriminatorMatchValue() );
		assertFalse( subclassOfSubclassEntityBinding.isRoot() );
		assertSame( subclassEntityBinding, subclassOfSubclassEntityBinding.getSuperEntityBinding() );
		assertSame( rootEntityBinding, getRootEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class ) );
		assertTrue( subclassOfSubclassEntityBinding.isPolymorphic() );
		assertFalse( subclassOfSubclassEntityBinding.hasSubEntityBindings() );
		assertEquals( 0, subclassOfSubclassEntityBinding.getSubEntityBindingClosureSpan() );
		assertFalse( subclassOfSubclassEntityBinding.getPostOrderSubEntityBindingClosure().length > 0 );
		assertFalse( subclassOfSubclassEntityBinding.getPreOrderSubEntityBindingClosure().length > 0 );
		Set<AttributeBinding> directAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : subclassOfSubclassEntityBinding.attributeBindings() ) {
			assertTrue( directAttributeBindings.add( attributeBinding ) );
		}
		assertEquals( 1, directAttributeBindings.size() );
		assertTrue( directAttributeBindings.contains( subclassOfSubclassEntityBinding.locateAttributeBinding( "otherOtherName" ) ) );
		assertEquals( 3, subclassOfSubclassEntityBinding.getAttributeBindingClosureSpan() );
		Set<AttributeBinding> attributeBindingClosure = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : subclassOfSubclassEntityBinding.getAttributeBindingClosure() ) {
			assertTrue( attributeBindingClosure.add( attributeBinding ) );
		}
		assertEquals( 3, attributeBindingClosure.size() );
		assertTrue( attributeBindingClosure.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( attributeBindingClosure.contains( subclassEntityBinding.locateAttributeBinding( "name" ) ) );
		assertTrue( attributeBindingClosure.contains( subclassOfSubclassEntityBinding.locateAttributeBinding( "otherOtherName" ) ) );
		Set<AttributeBinding> subAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding subAttributeBinding : subclassOfSubclassEntityBinding.getEntitiesAttributeBindingClosure() ) {
			assertTrue( subAttributeBindings.add( subAttributeBinding ) );
		}
		assertEquals( 3, subAttributeBindings.size() );
		assertTrue( subAttributeBindings.contains( rootEntityBinding.locateAttributeBinding( "id" ) ) );
		assertTrue( subAttributeBindings.contains( subclassEntityBinding.locateAttributeBinding( "name" ) ) );
		assertTrue( subAttributeBindings.contains( subclassOfSubclassEntityBinding.locateAttributeBinding( "otherOtherName" ) ) );
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

	@Test
	@Resources(annotatedClasses = { Base.class, Jump.class })
	public void testRootDiscriminatorMatchValue() {
		EntityBinding rootEntityBinding = getEntityBinding( Base.class );
		assertEquals( "base", rootEntityBinding.getDiscriminatorMatchValue() );
	}

	@Test
	@Resources(annotatedClasses = { Fruit.class, Apple.class })
	public void testDiscriminatorFormula() {
		EntityBinding rootEntityBinding = getEntityBinding( Fruit.class );
		assertTrue( rootEntityBinding.isRoot() );
		EntityBinding entityBinding = getEntityBinding( Apple.class );
		assertFalse( entityBinding.isRoot() );
		EntityDiscriminator discriminator = rootEntityBinding.getHierarchyDetails().getEntityDiscriminator();
		Value simpleValue = discriminator.getRelationalValue();
		assertTrue( simpleValue instanceof DerivedValue);
		DerivedValue derivedValue = (DerivedValue)simpleValue;
		assertEquals( "case when zik_type is null then 0 else zik_type end", derivedValue.getExpression() );
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
	@DiscriminatorValue("foo1")
	public class SubclassOfSingleTableInheritance extends RootOfSingleTableInheritance {
		private String name;
	}

	@Entity
	@DiscriminatorValue("foo2")
	public class OtherSubclassOfSingleTableInheritance extends RootOfSingleTableInheritance {
		private String otherName;
	}

	@Entity
	@DiscriminatorValue("foo1_1")
	public class SubclassOfSubclassOfSingleTableInheritance extends SubclassOfSingleTableInheritance {
		private String otherOtherName;
	}

	@Entity
	@DiscriminatorValue("base")
	@DiscriminatorOptions(force = true, insert = false)
	class Base {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	class Jump extends Base {
	}

	@Entity
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER)
	@DiscriminatorFormula("case when zik_type is null then 0 else zik_type end")
	@DiscriminatorOptions(force = true, insert = false)
	class Fruit {
		@Id
		private int id;
	}

	@Entity
	class Apple extends Fruit {

	}

}


