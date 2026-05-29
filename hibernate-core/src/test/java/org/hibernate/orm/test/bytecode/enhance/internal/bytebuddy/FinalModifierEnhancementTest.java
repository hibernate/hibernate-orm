/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.ClassFileLocator.Compound;
import net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader;
import net.bytebuddy.pool.TypePool;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;

import org.hibernate.annotations.Immutable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.assertj.core.api.Assertions.assertThat;

public class FinalModifierEnhancementTest {

	@ParameterizedTest
	@ValueSource(classes = { FinalEntity.class, FinalMappedSuperclass.class })
	void finalModifiersAreRemovedFromManagedTypesAndMethods(Class<?> testClass) {
		final var typeDescription = enhanceAndDescribe( testClass );

		assertThat( typeDescription.isFinal() ).isFalse();
		assertMethodIsNotFinal( typeDescription, "getId" );
		assertMethodIsNotFinal( typeDescription, "setId" );
		assertMethodIsNotFinal( typeDescription, "helper" );
		assertMethodIsNotFinal( typeDescription, "staticHelper" );
	}

	@Test
	void finalImmutableBasicFieldsAreMarkedImmutable() {
		final var typeDescription = enhanceAndDescribe( FinalImmutableBasicFields.class );

		assertImmutable( typeDescription, "primitiveValue" );
		assertImmutable( typeDescription, "wrapperValue" );
		assertImmutable( typeDescription, "stringValue" );
		assertImmutable( typeDescription, "localDate" );
		assertImmutable( typeDescription, "instant" );
		assertImmutable( typeDescription, "bigDecimal" );
		assertImmutable( typeDescription, "bigInteger" );

		assertNotImmutable( typeDescription, "stringArray" );
		assertNotImmutable( typeDescription, "list" );
		assertNotImmutable( typeDescription, "utilDate" );
		assertNotImmutable( typeDescription, "manyToOne" );
		assertNotImmutable( typeDescription, "regular" );
	}

	@Test
	void finalMappedSuperclassImmutableBasicFieldsAreMarkedImmutable() {
		final var typeDescription = enhanceAndDescribe( FinalImmutableBasicMappedSuperclass.class );

		assertImmutable( typeDescription, "mappedString" );
		assertNotImmutable( typeDescription, "mappedDate" );
	}

	private static void assertMethodIsNotFinal(TypeDescription typeDescription, String methodName) {
		assertThat( typeDescription.getDeclaredMethods().filter( named( methodName ) ).getOnly().isFinal() )
				.isFalse();
	}

	private static void assertImmutable(TypeDescription typeDescription, String fieldName) {
		assertThat( field( typeDescription, fieldName ).getDeclaredAnnotations().ofType( Immutable.class ) )
				.isNotNull();
	}

	private static void assertNotImmutable(TypeDescription typeDescription, String fieldName) {
		assertThat( field( typeDescription, fieldName ).getDeclaredAnnotations().ofType( Immutable.class ) )
				.isNull();
	}

	private static FieldDescription.InDefinedShape field(TypeDescription typeDescription, String fieldName) {
		return typeDescription.getDeclaredFields().filter( named( fieldName ) ).getOnly();
	}

	private static TypeDescription enhanceAndDescribe(Class<?> clazz) {
		final Enhancer enhancer = new EnhancerImpl( new DefaultEnhancementContext(), new ByteBuddyState() );
		final byte[] enhancedBytes = enhance( clazz, enhancer );

		try (final var enhancedClassLocator = ClassFileLocator.Simple.of( clazz.getName(), enhancedBytes );
				final var classLoaderLocator = ForClassLoader.of( FinalModifierEnhancementTest.class.getClassLoader() );
				final var locator = new Compound( enhancedClassLocator, classLoaderLocator )) {
			return TypePool.Default.of( locator ).describe( clazz.getName() ).resolve();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static byte[] enhance(Class<?> clazz, Enhancer enhancer) {
		final String classFileName = clazz.getName().replace( '.', '/' ) + ".class";
		try (var classFileStream = requireNonNull( clazz.getClassLoader().getResourceAsStream( classFileName ) ) ) {
			return enhancer.enhance( clazz.getName(), ByteCodeHelper.readByteCode( classFileStream ) );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Entity
	static final class FinalEntity {
		@Id
		private Long id;

		final Long getId() {
			return id;
		}

		final void setId(Long id) {
			this.id = id;
		}

		final String helper() {
			return "entity";
		}

		static final String staticHelper() {
			return "entity";
		}
	}

	@MappedSuperclass
	static final class FinalMappedSuperclass {
		@Id
		private Long id;

		final Long getId() {
			return id;
		}

		final void setId(Long id) {
			this.id = id;
		}

		final String helper() {
			return "mapped-superclass";
		}

		static final String staticHelper() {
			return "mapped-superclass";
		}
	}

	@Entity
	static class FinalImmutableBasicFields {
		@Id
		private Long id;

		private final int primitiveValue = 1;

		private final Integer wrapperValue = 1;

		private final String stringValue = "";

		private final LocalDate localDate = LocalDate.EPOCH;

		private final Instant instant = Instant.EPOCH;

		private final BigDecimal bigDecimal = BigDecimal.ONE;

		private final BigInteger bigInteger = BigInteger.ONE;

		private final String[] stringArray = {};

		private final List<String> list = List.of();

		private final Date utilDate = new Date();

		@ManyToOne
		private final AssociatedEntity manyToOne = null;

		private String regular;
	}

	@MappedSuperclass
	static class FinalImmutableBasicMappedSuperclass {
		private final String mappedString = "";

		private final Date mappedDate = new Date();
	}

	@Entity
	static class AssociatedEntity {
		@Id
		private Long id;
	}
}
