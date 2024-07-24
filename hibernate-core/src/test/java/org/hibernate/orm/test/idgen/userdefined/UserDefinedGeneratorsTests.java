/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanContainer.LifecycleOptions;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * @author Yanming Zhou
 */
@JiraKey(value = "HHH-14688")
@FailureExpected(reason = "functionality has been removed for now")
@BaseUnitTest
public class UserDefinedGeneratorsTests {

	@Test
	public void testCreateGeneratorsByBeanContainer() {

		final BeanContainer beanContainer = Mockito.mock( BeanContainer.class );
		given(beanContainer.getBean( any(), any(), any() ) ).willAnswer( invocation -> {
			Class<?> beanType = (Class<?>) invocation.getArguments()[0];
			LifecycleOptions options = (LifecycleOptions) invocation.getArguments()[1];
			if (beanType == TestIdentifierGenerator.class) {
				assertThat( options.canUseCachedReferences(), is( false ) );
				assertThat( options.useJpaCompliantCreation(), is( true ) );
				return (ContainedBean<?>) TestIdentifierGenerator::new;
			}
			else {
				return (ContainedBean<?>) () -> ( ( BeanInstanceProducer ) invocation.getArguments()[2] ).produceBeanInstance( beanType );
			}
		} );

		final StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.BEAN_CONTAINER, beanContainer )
				.applySetting( AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, "true" );

		try (final StandardServiceRegistry ssr = ssrb.build()) {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Entity1.class )
					.addAnnotatedClass( Entity2.class )
					.buildMetadata();

			final PersistentClass entityBinding1 = metadata.getEntityBinding( Entity1.class.getName() );
			final PersistentClass entityBinding2 = metadata.getEntityBinding( Entity2.class.getName() );
			KeyValue keyValue1 = entityBinding1.getRootClass()
					.getIdentifier();
			Dialect dialect1 = new H2Dialect();
			final Generator generator3 = keyValue1.createGenerator(dialect1, entityBinding1.getRootClass());
			final IdentifierGenerator generator1 = generator3 instanceof IdentifierGenerator ? (IdentifierGenerator) generator3 : null;
			KeyValue keyValue = entityBinding2.getRootClass()
					.getIdentifier();
			Dialect dialect = new H2Dialect();
			final Generator generator = keyValue.createGenerator( dialect, entityBinding2.getRootClass());
			final IdentifierGenerator generator2 = generator instanceof IdentifierGenerator ? (IdentifierGenerator) generator : null;

			then( beanContainer ).should( times( 2 ) ).getBean( same( TestIdentifierGenerator.class ),
																any( LifecycleOptions.class ),
																same( FallbackBeanInstanceProducer.INSTANCE )
			);

			assertThat( generator1, is( instanceOf( TestIdentifierGenerator.class ) ) );
			assertThat( generator2, is( instanceOf( TestIdentifierGenerator.class ) ) );
			assertThat( generator1 == generator2, is( false ) ); // should not be same instance
		}

	}

	@Entity( name = "Entity1" )
	@Table( name = "tbl_1" )
	public static class Entity1 {
		@Id
		@GeneratedValue( generator = "test" )
		@GenericGenerator( name = "test", strategy = "org.hibernate.orm.test.idgen.userdefined.UserDefinedGeneratorsTests$TestIdentifierGenerator" )
		private Integer id;
	}

	@Entity( name = "Entity2" )
	@Table( name = "tbl_2" )
	public static class Entity2 {
		@Id
		@GeneratedValue( generator = "test" )
		@GenericGenerator( name = "test", strategy = "org.hibernate.orm.test.idgen.userdefined.UserDefinedGeneratorsTests$TestIdentifierGenerator" )
		private Integer id;
	}

	public static class TestIdentifierGenerator implements IdentifierGenerator {

		private AtomicInteger count = new AtomicInteger();

		@Override
		public Serializable generate( SharedSessionContractImplementor session, Object obj ) {
			return count.incrementAndGet();
		}

	}

}
