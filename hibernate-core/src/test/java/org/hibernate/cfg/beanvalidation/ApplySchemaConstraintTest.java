/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.beanvalidation;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.service.BootstrapServiceRegistry;
import org.hibernate.service.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.validator.constraints.Length;

import static junit.framework.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */

public class ApplySchemaConstraintTest {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = createServiceRegistry();
	}

	@Test
	public void testLengthConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Foo.class.getName() ), "s" );
		assertEquals( "@Length constraint should have been applied", 10, column.getSize().getLength() );
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class,
		};
	}

	private Column getColumnForAttribute(EntityBinding entityBinding, String propertyName) {
		AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( propertyName );
		BasicAttributeBinding basicAttributeBinding = ( BasicAttributeBinding ) attributeBinding;
		RelationalValueBinding valueBinding = basicAttributeBinding.getRelationalValueBindings().get( 0 );
		Value value = valueBinding.getValue();
		return ( org.hibernate.metamodel.spi.relational.Column ) value;
	}

	private StandardServiceRegistryImpl createServiceRegistry() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		final BootstrapServiceRegistry bootstrapServiceRegistry = builder.build();
		ServiceRegistryBuilder registryBuilder = new ServiceRegistryBuilder( bootstrapServiceRegistry );
		return ( StandardServiceRegistryImpl ) registryBuilder.buildServiceRegistry();
	}

	private MetadataImplementor buildMetadata(ServiceRegistry serviceRegistry) {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
		}
		return ( MetadataImplementor ) sources.buildMetadata();
	}

	@Entity
	public static class Foo {
		@Id
		@GeneratedValue
		private int id;

		@Length(max = 10)
		private String s;

	}
}
