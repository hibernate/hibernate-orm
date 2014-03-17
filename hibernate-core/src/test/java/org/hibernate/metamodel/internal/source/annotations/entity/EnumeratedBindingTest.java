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

import java.sql.Types;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Strong Liu
 */
public class EnumeratedBindingTest extends BaseAnnotationBindingTestCase {
	@Entity
	class Item {
		@Id
		long id;
		@Temporal(TemporalType.TIMESTAMP)
		Date orderDate;
		String name;
		@Enumerated(EnumType.STRING)
		OrderType orderType;
		CustomerType customerType;
	}

	enum CustomerType {
		PROGRAMMER, BOSS;
	}

	enum OrderType {
		B2C, C2C, MAIL, DIRECT;
	}

	@Test
	@Resources(annotatedClasses = Item.class)
	public void testEnumeratedTypeAttribute() {
		EntityBinding binding = getEntityBinding( Item.class );

		AttributeBinding attributeBinding = binding.locateAttributeBinding( "customerType" );
		HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
		assertEquals( org.hibernate.type.EnumType.class.getName(), descriptor.getExplicitTypeName() );
		assertEquals( CustomerType.class.getName(), descriptor.getJavaTypeDescriptor().getName().toString() );
		assertNotNull( descriptor.getResolvedTypeMapping() );
		assertFalse( descriptor.getTypeParameters().isEmpty() );
		assertEquals(
				CustomerType.class.getName(),
				descriptor.getTypeParameters().get( DynamicParameterizedType.RETURNED_CLASS )
		);
		assertEquals(
				String.valueOf( Types.INTEGER ),
				descriptor.getTypeParameters().get( org.hibernate.type.EnumType.TYPE )
		);


		attributeBinding = binding.locateAttributeBinding( "orderType" );
		descriptor = attributeBinding.getHibernateTypeDescriptor();
		assertEquals( org.hibernate.type.EnumType.class.getName(), descriptor.getExplicitTypeName() );
		assertEquals( OrderType.class.getName(), descriptor.getJavaTypeDescriptor().getName().toString() );
		assertNotNull( descriptor.getResolvedTypeMapping() );
		assertFalse( descriptor.getTypeParameters().isEmpty() );
		assertEquals(
				OrderType.class.getName(),
				descriptor.getTypeParameters().get( DynamicParameterizedType.RETURNED_CLASS )
		);
		assertEquals(
				String.valueOf( Types.VARCHAR ),
				descriptor.getTypeParameters().get( org.hibernate.type.EnumType.TYPE )
		);
	}
}
