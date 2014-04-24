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
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.hibernate.metamodel.spi.binding.BindingHelper.locateAttributeBinding;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Assertions about the metamodel interpretations of basic {@link javax.persistence.EmbeddedId} usage.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.metamodel.spi.binding.cid.BasicIdClassTest
 */
public class BasicEmbeddedIdTest extends BaseAnnotationBindingTestCase {
	@Embeddable
	public static class CoursePK {
		@Column( name = "dept" )
		public String department;
		public String code;
	}

	@Entity
	public static class Course {
		@EmbeddedId
		private CoursePK key;
		private String title;
	}

	@Test
	@Resources( annotatedClasses = {CoursePK.class, Course.class} )
	public void testBasicUsage() {
		// get the Course entity binding
		EntityBinding courseBinding = getEntityBinding( Course.class );
		assertNotNull( courseBinding );
		assertEquals(
				EntityIdentifierNature.AGGREGATED_COMPOSITE,
				courseBinding.getHierarchyDetails().getEntityIdentifier().getNature()
		);
		assertFalse(
				courseBinding.getHierarchyDetails().getEntityIdentifier().definesIdClass()
		);

		// Course should be interpreted as defining 2 attributes: `key` and `title`
		assertEquals( 2, courseBinding.getAttributeBindingClosureSpan() );

		// just make sure `title` is one of them
		locateAttributeBinding( courseBinding, "title", BasicAttributeBinding.class );

		// locate the attribute binding for `key` which is the EmbeddedId attribute
		EmbeddedAttributeBinding keyBinding = locateAttributeBinding(
				courseBinding,
				"key",
				EmbeddedAttributeBinding.class
		);
		SingularAttributeBinding identifierAttribute =  courseBinding.getHierarchyDetails()
				.getEntityIdentifier()
				.getEntityIdentifierBinding()
				.getAttributeBinding();
		// NOTE : assertSame() does '=='
		assertSame( keyBinding, identifierAttribute );

		// the Embeddable for `key` (CoursePK) should also define 2 attributes: `department` and `code`
		assertEquals( 2, keyBinding.getEmbeddableBinding().attributeBindingSpan() );

		BasicAttributeBinding deptAttribute = locateAttributeBinding(
				keyBinding.getEmbeddableBinding(),
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
				keyBinding.getEmbeddableBinding(),
				"code",
				BasicAttributeBinding.class
		);
		assertEquals( 1, codeAttribute.getRelationalValueBindings().size() );
		RelationalValueBinding codeColumnBinding = codeAttribute.getRelationalValueBindings().get( 0 );
		org.hibernate.metamodel.spi.relational.Column codeColumn = assertTyping(
				org.hibernate.metamodel.spi.relational.Column.class,
				codeColumnBinding.getValue()
		);
		assertEquals( "code", codeColumn.getColumnName().getText() );

	}

}
