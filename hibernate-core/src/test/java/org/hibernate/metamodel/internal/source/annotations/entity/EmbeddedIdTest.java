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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.junit.Test;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class EmbeddedIdTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = { User.class, Address.class })
	public void testEmbeddable() {
		EntityBinding binding = getEntityBinding( User.class );
		EntityIdentifier identifier = binding.getHierarchyDetails().getEntityIdentifier();
		assertEquals( EntityIdentifierNature.AGGREGATED_COMPOSITE, identifier.getNature() );
	}

	@Entity
	@Access(AccessType.FIELD)
	class User {
		private String name;
		@EmbeddedId
		private Address address;
	}

	@Embeddable
	class Address {
		String street;
		String city;
		String postCode;
	}
}



