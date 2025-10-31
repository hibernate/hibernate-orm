/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the @Timestamp annotation.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class TimestampTest {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeAll
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistry();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( VMTimestamped.class )
				.addAnnotatedClass( DBTimestamped.class )
				.getMetadataBuilder()
				.build();
	}

	@AfterAll
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testTimestampSourceIsVM() throws Exception {
		assertTimestampSource( VMTimestamped.class, StandardBasicTypes.TIMESTAMP );
	}

	@Test
	public void testTimestampSourceIsDB() throws Exception {
		assertTimestampSource( DBTimestamped.class, StandardBasicTypes.TIMESTAMP );
	}

	private void assertTimestampSource(Class<?> clazz, BasicTypeReference<?> typeReference) throws Exception {
		assertTimestampSource( clazz, metadata.getTypeConfiguration().getBasicTypeRegistry().resolve( typeReference ) );
	}

	private void assertTimestampSource(Class<?> clazz, BasicType<?> basicType) throws Exception {
		PersistentClass persistentClass = metadata.getEntityBinding( clazz.getName() );
		assertThat( persistentClass ).isNotNull();
		Property versionProperty = persistentClass.getVersion();
		assertThat( versionProperty ).isNotNull();
		assertThat( versionProperty.getType() )
				.describedAs( "Wrong timestamp type" )
				.isEqualTo( basicType );
	}
}
