/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.namingstrategy.collectionJoinTableNaming;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 * @author Alessandro Polverini
 */
public class CollectionJoinTableNamingTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingBase() {
		// really the same as the JPA compliant tests; here we just pick up the default ImplicitNamingStrategy
		final MetadataSources metadataSources = new MetadataSources();
		try {
			metadataSources.addAnnotatedClass( Input.class );
			metadataSources.addAnnotatedClass( Ptx.class );

			final Metadata metadata = metadataSources.getMetadataBuilder()
					.build();

			assertSameTableUsed( metadata );
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingLegacyJpaStrategy() {
		final MetadataSources metadataSources = new MetadataSources();
		try {
			metadataSources.addAnnotatedClass( Input.class );
			metadataSources.addAnnotatedClass( Ptx.class );

			final Metadata metadata = metadataSources.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE )
					.build();

			assertSameTableUsed( metadata );
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingLegacyHbmStrategy() {
		final MetadataSources metadataSources = new MetadataSources();
		try {
			metadataSources.addAnnotatedClass( Input.class );
			metadataSources.addAnnotatedClass( Ptx.class );

			final Metadata metadata = metadataSources.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyLegacyHbmImpl.INSTANCE )
					.build();

			Collection inputs1Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs1" );
			assertEquals( "ptx_inputs1", inputs1Mapping.getCollectionTable().getName() );

			Collection inputs2Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs2" );
			assertEquals( "ptx_inputs2", inputs2Mapping.getCollectionTable().getName() );
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9908" )
	public void testCollectionJoinTableNamingJpaCompliantStrategy() {
		// Even in 4.3, with JPA compliant naming, Hibernate creates an unusable table...

		final MetadataSources metadataSources = new MetadataSources();
		try {
			metadataSources.addAnnotatedClass( Input.class );
			metadataSources.addAnnotatedClass( Ptx.class );

			final Metadata metadata = metadataSources.getMetadataBuilder()
					.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
					.build();

			assertSameTableUsed( metadata );
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	private void assertSameTableUsed(Metadata metadata) {
		Collection inputs1Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs1" );
		assertEquals( "ptx_input", inputs1Mapping.getCollectionTable().getName() );

		Collection inputs2Mapping = metadata.getCollectionBinding( Ptx.class.getName() + ".inputs2" );
		assertEquals( "ptx_input", inputs2Mapping.getCollectionTable().getName() );

		assertSame( inputs1Mapping.getCollectionTable(), inputs2Mapping.getCollectionTable() );

		// NOTE : here so that tester can more easily see the produced table. It is only dumped to stdout
		new SchemaExport().create( EnumSet.of( TargetType.STDOUT ), metadata );

		for ( int i = 0; i < inputs1Mapping.getCollectionTable().getColumnSpan(); i++ ) {
			final Column column = inputs1Mapping.getCollectionTable().getColumn( i );

			// this, coupled with JPA saying the 2 collections implicitly map to the same table,
			// is the crux of the problem: all columns are null, so we effectively can never
			// insert rows into it.
			assertFalse( column.isNullable() );
		}
	}

	@Entity
	@Table(name = "ptx")
	public static class Ptx {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		private Integer id;

		@OrderColumn
		@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		private List<Input> inputs1;

		@OrderColumn
		@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		private List<Input> inputs2;

	}

	@Entity
	@Table(name = "input")
	public class Input implements Serializable {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		private Integer id;
	}
}
