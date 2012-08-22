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
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.metamodel.spi.relational.Value;
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
		MetadataImplementor metadata = buildMetadata( serviceRegistry, Foo.class );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Foo.class.getName() ), "s" );
		assertEquals( "@Length constraint should have been applied", 10, column.getSize().getLength() );
	}

	@Test
	public void testDigitsConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry, Fubar.class );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Fubar.class.getName() ), "f" );
		Size size = column.getSize();
		assertEquals( "@Digits should have been applied", 1, size.getScale() );
		assertEquals( "@Digits should have been applied", 2, size.getPrecision() );
	}

	@Test
	public void testMinConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry, Foobar.class );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Foobar.class.getName() ), "i" );
		assertEquals( "@Min constraint should have been applied", "i>=42", column.getCheckCondition() );
	}

	@Test
	public void testMaxConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry, Snafu.class );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Snafu.class.getName() ), "i" );
		assertEquals( "@Max constraint should have been applied", "i<=42", column.getCheckCondition() );
	}

	@Test
	public void testSizeConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry, Tarfu.class );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Tarfu.class.getName() ), "s" );
		Size size = column.getSize();
		assertEquals( "@Size constraint should have been applied", 42, size.getLength() );
	}

	@Test
	public void testNotNullConstraintApplied() throws Exception {
		MetadataImplementor metadata = buildMetadata( serviceRegistry, Bohica.class );
		metadata.buildSessionFactory();

		Column column = getColumnForAttribute( metadata.getEntityBinding( Bohica.class.getName() ), "s" );
		assertEquals( "@NotNull constraint should have been applied", false, column.isNullable() );
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

	private MetadataImplementor buildMetadata(ServiceRegistry serviceRegistry, Class<?>... classes) {
		MetadataSources sources = new MetadataSources( serviceRegistry );

		for ( Class<?> annotatedClass : classes ) {
			sources.addAnnotatedClass( annotatedClass );

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

	@Entity
	public static class Fubar {
		@Id
		@GeneratedValue
		private int id;

		@Digits(integer = 1, fraction = 1)
		private float f;
	}

	@Entity
	public static class Foobar {
		@Id
		@GeneratedValue
		private int id;

		@Min(42)
		private int i;
	}

	@Entity
	public static class Snafu {
		@Id
		@GeneratedValue
		private int id;

		@Max(42)
		private int i;
	}

	@Entity
	public static class Tarfu {
		@Id
		@GeneratedValue
		private int id;

		@javax.validation.constraints.Size(max = 42)
		private String s;
	}

	@Entity
	public static class Bohica {
		@Id
		@GeneratedValue
		private int id;

		@NotNull
		private String s;
	}
}
