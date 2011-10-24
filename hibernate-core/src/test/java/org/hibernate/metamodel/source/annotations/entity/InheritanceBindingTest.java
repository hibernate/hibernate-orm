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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;

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
		assertFalse( noInheritanceEntityBinding.getPostOrderSubEntityBindingClosure().iterator().hasNext() );
		assertFalse( noInheritanceEntityBinding.getPreOrderSubEntityBindingClosure().iterator().hasNext() );
		Set<AttributeBinding> directAttributeBindings = new HashSet<AttributeBinding>();
		for ( AttributeBinding attributeBinding : noInheritanceEntityBinding.attributeBindings() ) {
			assertTrue( directAttributeBindings.add( attributeBinding ) );
		}
		assertEquals( 1, directAttributeBindings.size() );
		assertSame(
				noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding(),
				directAttributeBindings.iterator().next()
		);
		assertEquals( 1, noInheritanceEntityBinding.getAttributeBindingClosureSpan() );
		Iterator<AttributeBinding> iterator = noInheritanceEntityBinding.attributeBindings().iterator();
		assertTrue( iterator.hasNext() );
		assertSame( noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding(), iterator.next() );
		assertFalse( iterator.hasNext() );
		iterator = noInheritanceEntityBinding.getAttributeBindingClosure().iterator();
		assertTrue( iterator.hasNext() );
		assertSame( noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding(), iterator.next() );
		assertFalse( iterator.hasNext() );
		iterator =  noInheritanceEntityBinding.getSubEntityAttributeBindingClosure().iterator();
		assertTrue( iterator.hasNext() );
		assertSame( noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding(), iterator.next() );
		assertFalse( iterator.hasNext() );
	}

	@Test
	@Resources(annotatedClasses = {
			SubclassOfSingleTableInheritance.class,
			SingleEntity.class,
			RootOfSingleTableInheritance.class,
			OtherSubclassOfSingleTableInheritance.class,
			SubclassOfSubclassOfSingleTableInheritance.class
	})
	public void testRootPolymporhism() {
		EntityBinding rootEntityBinding = getEntityBinding( RootOfSingleTableInheritance.class );
		EntityBinding subclassEntityBinding = getEntityBinding( SubclassOfSingleTableInheritance.class );
		EntityBinding otherSubclassEntityBinding = getEntityBinding( OtherSubclassOfSingleTableInheritance.class );
		EntityBinding subclassOfSubclassEntityBinding = getEntityBinding( SubclassOfSubclassOfSingleTableInheritance.class );

		assertTrue( rootEntityBinding.isRoot() );
		assertNull( rootEntityBinding.getDiscriminatorMatchValue() );
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
		for ( AttributeBinding subAttributeBinding : rootEntityBinding.getSubEntityAttributeBindingClosure() ) {
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
		Iterator<EntityBinding> subEntityBindingIterator = rootEntityBinding.getPreOrderSubEntityBindingClosure().iterator();
		assertTrue( subEntityBindingIterator.hasNext() );
		if ( isSubclassEntityBindingFirst ) {
			assertSame( subclassEntityBinding, subEntityBindingIterator.next() );
			assertTrue( subEntityBindingIterator.hasNext() );
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator.next() );
			assertTrue( subEntityBindingIterator.hasNext() );
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator.next() );
		}
		else {
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator.next() );
			assertTrue( subEntityBindingIterator.hasNext() );
			assertSame( subclassEntityBinding, subEntityBindingIterator.next() );
			assertTrue( subEntityBindingIterator.hasNext() );
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator.next() );
		}
		assertFalse( subEntityBindingIterator.hasNext() );
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
		Iterator<EntityBinding> subEntityBindingIterator = rootEntityBinding.getPostOrderSubEntityBindingClosure().iterator();
		assertTrue( subEntityBindingIterator.hasNext() );
		if ( isSubclassEntityBindingFirst ) {
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator.next() );
			assertSame( subclassEntityBinding, subEntityBindingIterator.next() );
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator.next() );
		}
		else {
			assertSame( subclassOfSubclassEntityBinding, subEntityBindingIterator.next() );
			assertSame( otherSubclassEntityBinding, subEntityBindingIterator.next() );
			assertSame( subclassEntityBinding, subEntityBindingIterator.next() );
		}
		assertFalse( subEntityBindingIterator.hasNext() );
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
		assertFalse( otherSubclassEntityBinding.getPostOrderSubEntityBindingClosure().iterator().hasNext() );
		assertFalse( otherSubclassEntityBinding.getPreOrderSubEntityBindingClosure().iterator().hasNext() );
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
		for ( AttributeBinding subAttributeBinding : otherSubclassEntityBinding.getSubEntityAttributeBindingClosure() ) {
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
		Iterator<EntityBinding> itSubEntityBindings = subclassEntityBinding.getPostOrderSubEntityBindingClosure().iterator();
		assertTrue( itSubEntityBindings.hasNext() );
		assertSame( subclassOfSubclassEntityBinding, itSubEntityBindings.next() );
		assertFalse( itSubEntityBindings.hasNext() );
		itSubEntityBindings = subclassEntityBinding.getPreOrderSubEntityBindingClosure().iterator();
		assertTrue( itSubEntityBindings.hasNext() );
		assertSame( subclassOfSubclassEntityBinding, itSubEntityBindings.next() );
		assertFalse( itSubEntityBindings.hasNext() );
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
		for ( AttributeBinding subAttributeBinding : subclassEntityBinding.getSubEntityAttributeBindingClosure() ) {
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
		assertFalse( subclassOfSubclassEntityBinding.getPostOrderSubEntityBindingClosure().iterator().hasNext() );
		assertFalse( subclassOfSubclassEntityBinding.getPreOrderSubEntityBindingClosure().iterator().hasNext() );
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
		for ( AttributeBinding subAttributeBinding : subclassOfSubclassEntityBinding.getSubEntityAttributeBindingClosure() ) {
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
        SimpleValue simpleValue = discriminator.getBoundValue();
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


