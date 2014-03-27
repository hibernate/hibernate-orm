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

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class BasicEmbeddedIdTest extends BaseAnnotationBindingTestCase {
	@Embeddable
	public static class CoursePK {
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
		EntityBinding courseBinding = getEntityBinding( Course.class );
		assertEquals( 2, courseBinding.getAttributeBindingClosureSpan() );

		EmbeddedAttributeBinding keyBinding = locateAttributeBinding( courseBinding, "key", EmbeddedAttributeBinding.class );
		assertEquals( 2, keyBinding.getEmbeddableBinding().attributeBindingSpan() );

		assertEquals(
				EntityIdentifierNature.AGGREGATED_COMPOSITE,
				courseBinding.getHierarchyDetails().getEntityIdentifier().getNature()
		);
		SingularAttributeBinding identifierAttribute =  courseBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		// NOTE : same does '=='
		assertSame( keyBinding, identifierAttribute );

		BasicAttributeBinding titleBinding = locateAttributeBinding( courseBinding, "title", BasicAttributeBinding.class );
	}

	private <T extends AttributeBinding> T locateAttributeBinding(
			AttributeBindingContainer attributeContainer,
			String attributeName,
			Class<T> expectedType) {
		AttributeBinding attributeBinding = attributeContainer.locateAttributeBinding( attributeName );
		assertNotNull( "Could not locate attribute named " + attributeName, attributeBinding );
		return assertTyping( expectedType, attributeBinding );

	}

}
