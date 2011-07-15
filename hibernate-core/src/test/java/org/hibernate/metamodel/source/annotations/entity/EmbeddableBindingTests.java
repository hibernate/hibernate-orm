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

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.metamodel.binding.EntityBinding;

import org.hibernate.metamodel.domain.Component;
import org.hibernate.metamodel.domain.SingularAttribute;;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@code j.p.Embeddable}.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddableBindingTests extends BaseAnnotationBindingTestCase {
	@Test
	public void testEmbeddable() {
		buildMetadataSources( User.class, Address.class );
		EntityBinding binding = getEntityBinding( User.class );
		assertNotNull( binding.getAttributeBinding( "street" ) );
		assertNotNull( binding.getAttributeBinding( "city" ) );
		assertNotNull( binding.getAttributeBinding( "postCode" ) );

		SingularAttribute attribute = (SingularAttribute) binding.getEntity().getAttribute( "address" );
		assertTrue(
				"Wrong container type. Should be a component",
				attribute.getSingularAttributeType() instanceof Component
		);
	}

	@Entity
	class User {
		@Id
		private int id;

		@Embedded
		private Address address;
	}

	@Embeddable
	class Address {
		String street;
		String city;
		String postCode;
	}
}


