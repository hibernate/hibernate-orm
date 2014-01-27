package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.List;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.junit.Test;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class NaturalIdBindingTests extends BaseAnnotationBindingTestCase {
	@Entity
	public class SimpleEntityWithNaturalId {
		@Id
		long id;
		@NaturalId
		String name;
		@NaturalId(mutable = true)
		int age;
	}

	@Test
	@Resources(annotatedClasses = NaturalIdBindingTests.SimpleEntityWithNaturalId.class)
	public void testSimpleNaturalIdAttributeBinding() {
		EntityBinding entityBinding = getEntityBinding( SimpleEntityWithNaturalId.class );
		assertNull( entityBinding.getHierarchyDetails().getNaturalIdCaching() );

		SingularAttributeBinding attributeBinding = (SingularAttributeBinding) entityBinding.locateAttributeBinding(
				"name"
		);
		assertEquals(
				SingularAttributeBinding.NaturalIdMutability.IMMUTABLE,
				attributeBinding.getNaturalIdMutability()
		);

		List<RelationalValueBinding> relationalValueBindings = attributeBinding.getRelationalValueBindings();
		assertEquals( 1, relationalValueBindings.size() );

		RelationalValueBinding relationalValueBinding = relationalValueBindings.get( 0 );

		assertFalse(
				"immutable (by default) natural id should not be included in the update list",
				relationalValueBinding.isIncludeInUpdate()
		);

		Column column = Column.class.cast( relationalValueBinding.getValue() );

//		assertFalse( "natural id column should not be nullable", column.isNullable() );

		//-------------------------------------------------------------------------------------------------------
		attributeBinding = (SingularAttributeBinding) entityBinding.locateAttributeBinding(
				"age"
		);
		assertEquals(
				SingularAttributeBinding.NaturalIdMutability.MUTABLE,
				attributeBinding.getNaturalIdMutability()
		);

		relationalValueBindings = attributeBinding.getRelationalValueBindings();
		assertEquals( 1, relationalValueBindings.size() );

		relationalValueBinding = relationalValueBindings.get( 0 );

		assertTrue(
				"mutable natural id should be included in the update list",
				relationalValueBinding.isIncludeInUpdate()
		);

		column = Column.class.cast( relationalValueBinding.getValue() );

//		assertFalse( "natural id column should not be nullable", column.isNullable() );
		// -----------------------------------------------------------------------------------------------------
		
		assertTrue( SchemaUtil.hasUniqueKeys( entityBinding.getPrimaryTable(), "age", "name" ) );
	}

	@Entity
	public class EntityWithEmbedded {
		@Id
		long id;
		@NaturalId
		@Embedded
		Component component;

	}

	@Embeddable
	public class Component {
		String name;
		String age;
	}

	@Test
	@Resources(annotatedClasses = { EntityWithEmbedded.class, Component.class })
	public void testEmbeddedNaturalIdAttributeBinding() {
		EntityBinding entityBinding = getEntityBinding( EntityWithEmbedded.class );
		assertNull( entityBinding.getHierarchyDetails().getNaturalIdCaching() );

		SingularAttributeBinding attributeBinding = (SingularAttributeBinding) entityBinding.locateAttributeBinding(
				"component"
		);
		assertEquals(
				SingularAttributeBinding.NaturalIdMutability.IMMUTABLE,
				attributeBinding.getNaturalIdMutability()
		);

		List<RelationalValueBinding> relationalValueBindings = attributeBinding.getRelationalValueBindings();
		for ( RelationalValueBinding valueBinding : relationalValueBindings ) {
			assertFalse(
					"immutable (by default) natural id should not be included in the update list",
					valueBinding.isIncludeInUpdate()
			);
			Column column = Column.class.cast( valueBinding.getValue() );
//			assertFalse( "natural id column should not be nullable", column.isNullable() );
		}
		
		assertTrue( SchemaUtil.hasUniqueKeys( entityBinding.getPrimaryTable(), "age", "name" ) );
	}

	@Entity
	@NaturalIdCache
	public class EntityWithAssociation {
		@Id
		long id;

		@NaturalId
		String name;

		@ManyToOne
		@NaturalId
		SimpleEntity simpleEntity;
	}

	@Entity
	public class SimpleEntity {
		@Id
		long id;
		String simpleName;
	}

	@Test
	@Resources(annotatedClasses = {
			EntityWithAssociation.class,
			SimpleEntity.class
	})
	public void testAssociationNaturalIdBinding() {
		EntityBinding entityBinding = getEntityBinding( EntityWithAssociation.class );
		assertNotNull( entityBinding.getHierarchyDetails().getNaturalIdCaching() );
		assertNotNull( entityBinding.getHierarchyDetails().getNaturalIdCaching().getRegion() );

		SingularAttributeBinding attributeBinding = (SingularAttributeBinding)entityBinding.locateAttributeBinding( "simpleEntity" );
		assertEquals( SingularAttributeBinding.NaturalIdMutability.IMMUTABLE, attributeBinding.getNaturalIdMutability() );

		List<RelationalValueBinding> relationalValueBindings = attributeBinding.getRelationalValueBindings();
		assertEquals( 1, relationalValueBindings.size() );
		RelationalValueBinding relationalValueBinding = relationalValueBindings.get( 0 );

		assertFalse(
				"immutable natural id should not be included in the update list",
				relationalValueBinding.isIncludeInUpdate()
		);

		Column column = Column.class.cast( relationalValueBinding.getValue() );

//		assertFalse( "natural id column should not be nullable", column.isNullable() );
		// -----------------------------------------------------------------------------------------------------
		
		assertTrue( SchemaUtil.hasUniqueKeys( entityBinding.getPrimaryTable(), "name", "simpleEntity_id" ) );

	}
}
