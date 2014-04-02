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
package org.hibernate.test.annotations.access.jpa;

import javax.persistence.Access;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.LenientPersistentAttributeMemberResolver;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static javax.persistence.AccessType.FIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test for {@link org.hibernate.metamodel.spi.LenientPersistentAttributeMemberResolver}
 *
 * @author Steve Ebersole
 */
public class LenientMemberResolutionTest extends BaseUnitTestCase {

	@Entity(name = "MyEntity")
	public static class MyEntity {
		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Access(FIELD)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Test
	public void testResolution() {
		MetadataSources sources = new MetadataSources()
				.addAnnotatedClass( MyEntity.class );

		// technically speaking the @Access(FIELD) annotation on a getter violates
		// the JPA spec (section 2.3.2).  The standard resolution strategy sees that
		// and throws an exception.
		//
		// the lenient strategy allows it

		try {
			sources.getMetadataBuilder().build();
			fail( "Was expecting an exception" );
		}
		catch (AnnotationException expected) {
			// expected
		}


		Metadata metadata = sources
				.getMetadataBuilder()
				.with( LenientPersistentAttributeMemberResolver.INSTANCE )
				.build();

		EntityBinding myEntityBinding = metadata.getEntityBinding( MyEntity.class.getName() );
		assertNotNull( myEntityBinding );
		AttributeBinding nameAttrBinding = myEntityBinding.locateAttributeBinding( "name" );
		assertEquals( "field", nameAttrBinding.getPropertyAccessorName() );
	}
}
