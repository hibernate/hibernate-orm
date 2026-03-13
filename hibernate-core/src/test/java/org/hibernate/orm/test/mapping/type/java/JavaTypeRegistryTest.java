/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 */
public class JavaTypeRegistryTest {

	@Test
	public void testGetJavaTypeDescriptorRegistry() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final JavaTypeRegistry registry = new JavaTypeRegistry( typeConfiguration );
		final JavaType<String> descriptor = registry.findDescriptor( String.class );
		assertThat( descriptor ).isInstanceOf( StringJavaType.class );
	}

	@Test
	public void testRegisterJavaTypeDescriptorRegistry(){
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final JavaTypeRegistry registry = new JavaTypeRegistry( typeConfiguration );
		registry.addDescriptor( new CustomJavaType() );
		final JavaType<?> descriptor = registry.findDescriptor( CustomType.class );
		assertThat( descriptor ).isInstanceOf( CustomJavaType.class );
	}

	@Test
	void shouldReturnSameObjectWhenParallelResolveDescriptor() {
		//given
		final var latch = new CountDownLatch( 1 );
		final var executorService = Executors.newFixedThreadPool( 2 );
		final var typeConfiguration = new TypeConfiguration();
		final var registry = new JavaTypeRegistry( typeConfiguration );
		Callable<JavaType<CustomType>> task = () -> {
			latch.await();
			return registry.resolveDescriptor( CustomType.class );
		};

		//when
		final var future1 = CompletableFuture.supplyAsync( () -> uncheck( task ), executorService );
		final var future2 = CompletableFuture.supplyAsync( () -> uncheck( task ), executorService );
		latch.countDown();

		//then
		final var combinedFuture = CompletableFuture.allOf( future1, future2 );
		combinedFuture.join();
		Assertions.assertThat( uncheck( future1 ) ).isSameAs( uncheck( future2 ) );
	}

	public static class CustomType {
	}

	public static class CustomJavaType implements JavaType<CustomType> {
		@Override
		public Class<CustomType> getJavaTypeClass() {
			return CustomType.class;
		}

		@Override
		public MutabilityPlan<CustomType> getMutabilityPlan() {
			return null;
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
			return null;
		}

		@Override
		public Comparator<CustomType> getComparator() {
			return null;
		}

		@Override
		public int extractHashCode(CustomType value) {
			return 0;
		}

		@Override
		public boolean areEqual(CustomType one, CustomType another) {
			return false;
		}

		@Override
		public String extractLoggableRepresentation(CustomType value) {
			return null;
		}

		@Override
		public String toString(CustomType value) {
			return null;
		}

		@Override
		public CustomType fromString(CharSequence string) {
			return null;
		}

		@Override
		public CustomType wrap(Object value, WrapperOptions options) {
			return null;
		}

		@Override
		public <X> X unwrap(CustomType value, Class<X> type, WrapperOptions options) {
			return null;
		}
	}

	private <T> T uncheck(Callable<T> callable) {
		try {
			return callable.call();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private <T> T uncheck(Future<T> future) {
		try {
			return future.get();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}
}
