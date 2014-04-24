/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding.cid;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.type.StringType;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.hibernate.metamodel.spi.binding.BindingHelper.locateAttributeBinding;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Assertions about the metamodel interpretations of basic {@link javax.persistence.IdClass} usage.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.metamodel.spi.binding.cid.BasicEmbeddedIdTest
 */
public class BasicIdClassTest extends BaseAnnotationBindingTestCase {
	public static class CoursePK {
		public String department;
		public String code;
	}

	@Entity
	@IdClass( CoursePK.class )
	public static class Course {
		@Id
		@Column( name = "dept" )
		public String department;
		@Id
		public String code;
		private String title;
	}

	@Test
	@Resources( annotatedClasses = Course.class )
	public void testBasicUsage() throws Exception {
		// get the Course entity binding
		EntityBinding courseBinding = getEntityBinding( Course.class );
		assertNotNull( courseBinding );
		assertEquals(
				EntityIdentifierNature.NON_AGGREGATED_COMPOSITE,
				courseBinding.getHierarchyDetails().getEntityIdentifier().getNature()
		);

		EntityIdentifier.NonAggregatedCompositeIdentifierBinding identifierBinding = assertTyping(
				EntityIdentifier.NonAggregatedCompositeIdentifierBinding.class,
				courseBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding()
		);
		assertNotNull( identifierBinding.getIdClassMetadata() );
		assertNotNull( identifierBinding.getIdClassMetadata().getIdClassType() );
		assertNotNull( identifierBinding.getIdClassMetadata().getEmbeddableBinding() );

		// Course should be interpreted as defining 3 attributes: `department`, `code` and `title`
		assertEquals( 3, courseBinding.getAttributeBindingClosureSpan() );

		// just make sure `title` is one of them
		locateAttributeBinding( courseBinding, "title", BasicAttributeBinding.class );

		BasicAttributeBinding deptAttribute = locateAttributeBinding(
				courseBinding,
				"department",
				BasicAttributeBinding.class
		);
		assertEquals( 1, deptAttribute.getRelationalValueBindings().size() );
		RelationalValueBinding deptColumnBinding = deptAttribute.getRelationalValueBindings().get( 0 );
		org.hibernate.metamodel.spi.relational.Column deptColumn = assertTyping(
				org.hibernate.metamodel.spi.relational.Column.class,
				deptColumnBinding.getValue()
		);
		assertEquals( "dept", deptColumn.getColumnName().getText() );

		BasicAttributeBinding codeAttribute = locateAttributeBinding(
				courseBinding,
				"code",
				BasicAttributeBinding.class
		);
		RelationalValueBinding codeColumnBinding = codeAttribute.getRelationalValueBindings().get( 0 );
		org.hibernate.metamodel.spi.relational.Column codeColumn = assertTyping(
				org.hibernate.metamodel.spi.relational.Column.class,
				codeColumnBinding.getValue()
		);
		assertEquals( "code", codeColumn.getColumnName().getText() );


		assertTrue(
				courseBinding.getHierarchyDetails().getEntityIdentifier().
						getEntityIdentifierBinding()
						.isIdentifierAttributeBinding( deptAttribute )
		);

		assertTrue(
				courseBinding.getHierarchyDetails().getEntityIdentifier()
						.getEntityIdentifierBinding()
						.isIdentifierAttributeBinding( codeAttribute )
		);

		// get the non-aggregated composite id (virtual) attribute
		EmbeddedAttributeBinding identifierAttribute = (EmbeddedAttributeBinding) courseBinding.getHierarchyDetails()
				.getEntityIdentifier()
				.getEntityIdentifierBinding()
				.getAttributeBinding();
		assertNotNull( identifierAttribute );
		assertTrue( identifierAttribute.getAttribute().isSynthetic() );

		EmbeddableBinding virtualEmbeddable = identifierAttribute.getEmbeddableBinding();
		assertEquals( 2, virtualEmbeddable.attributeBindingSpan() );

		for ( AttributeBinding subAttributeBinding : virtualEmbeddable.attributeBindings() ) {
			assertNotNull( subAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() );
			assertEquals( StringType.INSTANCE, subAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() );
		}

	}
}
