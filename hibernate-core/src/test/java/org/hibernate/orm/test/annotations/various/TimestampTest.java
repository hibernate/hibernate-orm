/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.BasicType;
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
	public void testTimestampSourceIsVM() {
		assertTimestampSource( VMTimestamped.class );
	}

	@Test
	public void testTimestampSourceIsDB() {
		assertTimestampSource( DBTimestamped.class );
	}

	private void assertTimestampSource(Class<?> clazz ) {
		assertTimestampSource( clazz,
				metadata.getTypeConfiguration().getBasicTypeRegistry()
						.resolve( StandardBasicTypes.TIMESTAMP ) );
	}

	private void assertTimestampSource(Class<?> clazz, BasicType<?> basicType) {
		var persistentClass = metadata.getEntityBinding( clazz.getName() );
		assertThat( persistentClass ).isNotNull();
		var versionProperty = persistentClass.getVersion();
		assertThat( versionProperty ).isNotNull();
		assertThat( versionProperty.getType().getName() )
				.describedAs( "Wrong timestamp type" )
				.isEqualTo( basicType.getName() );
	}
}
