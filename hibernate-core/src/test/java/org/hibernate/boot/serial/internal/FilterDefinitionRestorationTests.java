/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FilterDefinitionRestorationTests {
	@Test
	void resolverIsReconstructedFromConsumerEnvironment() throws Exception {
		final FilterDefinition definition = serializedDefinition();
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		final ManagedBeanRegistry beanRegistry = mock( ManagedBeanRegistry.class );
		final ManagedBean resolverBean = mock( ManagedBean.class );
		when( classLoaderAccess.classForName( TitleResolver.class.getName() ) )
				.thenReturn( (Class) TitleResolver.class );
		when( beanRegistry.getBean( TitleResolver.class ) ).thenReturn( resolverBean );
		when( resolverBean.getBeanInstance() ).thenReturn( new TitleResolver() );

		definition.reattachParameterResolvers( classLoaderAccess, beanRegistry );

		assertThat( definition.getParameterResolver( "title" ).get() ).isEqualTo( "Hibernate" );
	}

	@Test
	void missingResolverReportsFilterAndParameter() throws Exception {
		final FilterDefinition definition = serializedDefinition();
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		when( classLoaderAccess.classForName( TitleResolver.class.getName() ) )
				.thenThrow( new ClassLoadingException( "missing" ) );

		assertThatThrownBy( () -> definition.reattachParameterResolvers(
				classLoaderAccess,
				mock( ManagedBeanRegistry.class )
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "filter 'titleFilter'" )
				.hasMessageContaining( "parameter 'title'" )
				.hasMessageContaining( TitleResolver.class.getName() );
	}

	@Test
	void incompatibleResolverContractIsRejected() throws Exception {
		final FilterDefinition definition = serializedDefinition();
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		when( classLoaderAccess.classForName( TitleResolver.class.getName() ) )
				.thenReturn( (Class) String.class );

		assertThatThrownBy( () -> definition.reattachParameterResolvers(
				classLoaderAccess,
				mock( ManagedBeanRegistry.class )
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "filter 'titleFilter'" )
				.hasMessageContaining( "parameter 'title'" )
				.hasMessageContaining( Supplier.class.getName() );
	}

	@Test
	void resolverInstantiationFailureIsScoped() throws Exception {
		final FilterDefinition definition = serializedDefinition();
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		final ManagedBeanRegistry beanRegistry = mock( ManagedBeanRegistry.class );
		when( classLoaderAccess.classForName( TitleResolver.class.getName() ) )
				.thenReturn( (Class) TitleResolver.class );
		when( beanRegistry.getBean( TitleResolver.class ) )
				.thenThrow( new IllegalStateException( "cannot create bean" ) );

		assertThatThrownBy( () -> definition.reattachParameterResolvers(
				classLoaderAccess,
				beanRegistry
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "instantiate" )
				.hasMessageContaining( "filter 'titleFilter'" )
				.hasMessageContaining( "parameter 'title'" );
	}

	@Test
	void missingParameterTypeReportsFilterAndParameter() {
		final var recipe = new FilterDefinitionRestorationRecipe(
				"titleFilter",
				"title = :title",
				false,
				false,
				Map.of( "title", "org.example.MissingFilterType" ),
				Map.of()
		);
		final MetadataBuildingContext context = mock( MetadataBuildingContext.class );
		final BootstrapContext bootstrapContext = mock( BootstrapContext.class );
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		when( context.getBootstrapContext() ).thenReturn( bootstrapContext );
		when( bootstrapContext.getClassLoaderAccess() ).thenReturn( classLoaderAccess );
		when( classLoaderAccess.classForName( "org.example.MissingFilterType" ) )
				.thenThrow( new ClassLoadingException( "missing" ) );

		assertThatThrownBy( () -> recipe.resolve( context ) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "filter 'titleFilter'" )
				.hasMessageContaining( "parameter 'title'" )
				.hasMessageContaining( "org.example.MissingFilterType" );
	}

	private static FilterDefinition serializedDefinition() throws Exception {
		final ManagedBean resolverBean = mock( ManagedBean.class );
		when( resolverBean.getBeanClass() ).thenReturn( TitleResolver.class );
		final var definition = new FilterDefinition(
				"titleFilter",
				"title = :title",
				true,
				false,
				Map.of(),
				Map.of( "title", resolverBean )
		);
		final var bytes = new ByteArrayOutputStream();
		try ( var output = new ObjectOutputStream( bytes ) ) {
			output.writeObject( definition );
		}
		try ( var input = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) {
			return (FilterDefinition) input.readObject();
		}
	}

	private static class TitleResolver implements Supplier<String> {
		@Override
		public String get() {
			return "Hibernate";
		}
	}
}
