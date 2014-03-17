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
package org.hibernate.metamodel.internal.source.annotations.util;

import javax.persistence.Id;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class TypeDiscoveryTest extends BaseAnnotationIndexTestCase {

	@Test
	public void testImplicitAndExplicitType() {
//		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = createEntityHierarchies( Entity.class );
//		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );
//
//		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
//		ManagedTypeMetadata configuredClass = iter.next();
//
//		PersistentAttribute property = configuredClass.getMappedAttribute( "id" );
//		assertEquals( "Unexpected property type", int.class, property.getJavaType() );
//
//		property = configuredClass.getMappedAttribute( "string" );
//		assertEquals( "Unexpected property type", String.class, property.getJavaType() );
//
//		property = configuredClass.getMappedAttribute( "customString" );
//		assertEquals( "Unexpected property type", "my.custom.Type", property.getExplicitHibernateTypeName() );
//
//		Map<String, String> typeParameters = property.getExplicitHibernateTypeParameters();
//		assertEquals( "There should be a type parameter", "bar", typeParameters.get( "foo" ) );
	}

	@javax.persistence.Entity
	class Entity {
		@Id
		private int id;
		private String string;
		@Type(type = "my.custom.Type", parameters = { @Parameter(name = "foo", value = "bar") })
		private String customString;
	}
}