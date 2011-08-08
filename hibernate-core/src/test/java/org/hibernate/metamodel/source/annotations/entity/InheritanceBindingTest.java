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
		assertEquals( 0, noInheritanceEntityBinding.getSubEntityBindingSpan() );
		assertFalse( noInheritanceEntityBinding.getSubEntityBindingClosure().iterator().hasNext() );
		assertEquals( 1, noInheritanceEntityBinding.getAttributeBindingClosureSpan() );
		for ( AttributeBinding attributeBinding : noInheritanceEntityBinding.getAttributeBindingClosure() ) {
			if ( attributeBinding == noInheritanceEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding() ) {
				continue;
			}
		}
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
		assertEquals( 3, rootEntityBinding.getSubEntityBindingSpan() );
		Set<EntityBinding> subEntityBindings = new HashSet<EntityBinding>(  );
		for ( EntityBinding subEntityBinding : rootEntityBinding.getSubEntityBindingClosure() ) {
			subEntityBindings.add( subEntityBinding );
		}
		assertEquals( 3, subEntityBindings.size() );
		assertTrue( subEntityBindings.contains( subclassEntityBinding ) );
		assertTrue( subEntityBindings.contains( otherSubclassEntityBinding ) );
		assertTrue( subEntityBindings.contains( subclassOfSubclassEntityBinding ) );
		assertEquals( 1, rootEntityBinding.getAttributeBindingClosureSpan() );
		Set<String> attributeNames = new HashSet<String>();
		for ( AttributeBinding attributeBinding : rootEntityBinding.getAttributeBindingClosure() ) {
			attributeNames.add( attributeBinding.getAttribute().getName() );
		}
		assertEquals( 1, attributeNames.size() );
		assertTrue( attributeNames.contains( "id" ) );
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
		assertEquals( 0, otherSubclassEntityBinding.getSubEntityBindingSpan() );
		assertFalse( otherSubclassEntityBinding.getSubEntityBindingClosure().iterator().hasNext() );
		assertEquals( 2, otherSubclassEntityBinding.getAttributeBindingClosureSpan() );
		Set<String> attributeNames = new HashSet<String>();
		for ( AttributeBinding attributeBinding : otherSubclassEntityBinding.getAttributeBindingClosure() ) {
			attributeNames.add( attributeBinding.getAttribute().getName() );
		}
		assertEquals( 2, attributeNames.size() );
		assertTrue( attributeNames.contains( "id" ) );
		assertTrue( attributeNames.contains( "otherName" ) );
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
		assertEquals( 1, subclassEntityBinding.getSubEntityBindingSpan() );
		Iterator<EntityBinding> itSubEntityBindings = subclassEntityBinding.getSubEntityBindingClosure().iterator();
		assertTrue( itSubEntityBindings.hasNext() );
		assertSame( subclassOfSubclassEntityBinding, itSubEntityBindings.next() );
		assertFalse( itSubEntityBindings.hasNext() );
		assertEquals( 2, subclassEntityBinding.getAttributeBindingClosureSpan() );
		Set<String> attributeNames = new HashSet<String>();
		for ( AttributeBinding attributeBinding : subclassEntityBinding.getAttributeBindingClosure() ) {
			attributeNames.add( attributeBinding.getAttribute().getName() );
		}
		assertEquals( 2, attributeNames.size() );
		assertTrue( attributeNames.contains( "id" ) );
		assertTrue( attributeNames.contains( "name" ) );
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
		assertEquals( 0, subclassOfSubclassEntityBinding.getSubEntityBindingSpan() );
		assertFalse( subclassOfSubclassEntityBinding.getSubEntityBindingClosure().iterator().hasNext() );
		assertEquals( 3, subclassOfSubclassEntityBinding.getAttributeBindingClosureSpan() );
		Set<String> attributeNames = new HashSet<String>();
		for ( AttributeBinding attributeBinding : subclassOfSubclassEntityBinding.getAttributeBindingClosure() ) {
			attributeNames.add( attributeBinding.getAttribute().getName() );
		}
		assertEquals( 3, attributeNames.size() );
		assertTrue( attributeNames.contains( "id" ) );
		assertTrue( attributeNames.contains( "name" ) );
		assertTrue( attributeNames.contains( "otherOtherName" ) );
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


