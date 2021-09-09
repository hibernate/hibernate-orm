/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.userdefined;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanContainer.LifecycleOptions;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Yanming Zhou
 */
@TestForIssue(jiraKey = "HHH-14688")
public class UserDefinedGeneratorsTests extends BaseUnitTestCase {

	@Test
	public void testCreateGeneratorsByBeanContainer() {
		
		final BeanContainer beanContainer = Mockito.mock( BeanContainer.class );
		given(beanContainer.getBean( any(), any(), any() ) ).willAnswer( invocation -> {
			LifecycleOptions options = (LifecycleOptions) invocation.getArguments()[1];
			assertThat( options.canUseCachedReferences(), is( false ) );
			assertThat( options.useJpaCompliantCreation(), is( true ) );
			return (ContainedBean<?>) TestIdentifierGenerator::new;
		} );
		
		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.BEAN_CONTAINER, beanContainer );

		try (final StandardServiceRegistry ssr = ssrb.build()) {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Entity1.class )
					.addAnnotatedClass( Entity2.class )
					.buildMetadata();

			final DefaultIdentifierGeneratorFactory generatorFactory = new DefaultIdentifierGeneratorFactory();
			generatorFactory.injectServices( (ServiceRegistryImplementor) ssr );

			final PersistentClass entityBinding1 = metadata.getEntityBinding( Entity1.class.getName() );
			final PersistentClass entityBinding2 = metadata.getEntityBinding( Entity2.class.getName() );
			final IdentifierGenerator generator1 = entityBinding1.getRootClass()
					.getIdentifier()
					.createIdentifierGenerator(
							generatorFactory,
							new H2Dialect(),
							"",
							"",
							entityBinding1.getRootClass()
					);
			final IdentifierGenerator generator2 = entityBinding2.getRootClass()
					.getIdentifier()
					.createIdentifierGenerator(
							generatorFactory,
							new H2Dialect(),
							"",
							"",
							entityBinding2.getRootClass()
					);

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
		@GenericGenerator( name = "test", strategy = "org.hibernate.test.idgen.userdefined.UserDefinedGeneratorsTests$TestIdentifierGenerator" )
		private Integer id;
	}
	
	@Entity( name = "Entity2" )
	@Table( name = "tbl_2" )
	public static class Entity2 {
		@Id
		@GeneratedValue( generator = "test" )
		@GenericGenerator( name = "test", strategy = "org.hibernate.test.idgen.userdefined.UserDefinedGeneratorsTests$TestIdentifierGenerator" )
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
