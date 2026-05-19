/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.n_ative;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.NativeGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel(
		annotatedClasses = {
				NativeGeneratorTypeSequenceTest.DefaultIncrSizeDefaultStart.class,
				NativeGeneratorTypeSequenceTest.IncrSizeOneStartOne.class,
				NativeGeneratorTypeSequenceTest.IncrSizeFiveStartTen.class
		}
)
public class NativeGeneratorTypeSequenceTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@BeforeAll
	static void setUp(SessionFactoryScope scope) {
		Assumptions.assumeTrue( dialectDefaultsToSequenceGenerator( scope ),
				"sequence generator is not the default for this dialect" );
	}

	static class TestArguments implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
			return Stream.of(
					Arguments.of( DefaultIncrSizeDefaultStart.class, 1, 50 ),
					Arguments.of( IncrSizeOneStartOne.class, 1, 1 ),
					Arguments.of( IncrSizeFiveStartTen.class, 10, 5 )
			);
		}
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	@JiraKey(value = "HHH-20421")
	public void testNormalBoundary(Class<?> entityClass, int expectedInitialValue, int expectedAllocationSize, SessionFactoryScope scope)
			throws Exception {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( entityClass.getName() );
		assertThat( persister.getGenerator() ).isInstanceOf( org.hibernate.id.NativeGenerator.class );

		final org.hibernate.id.NativeGenerator generator = (org.hibernate.id.NativeGenerator) persister.getGenerator();
		SequenceStyleGenerator sequenceStyleGenerator = getSequenceStyleGenerator( generator );

		assertEquals( expectedInitialValue, sequenceStyleGenerator.getDatabaseStructure().getInitialValue() );
		assertEquals( expectedAllocationSize, sequenceStyleGenerator.getDatabaseStructure().getIncrementSize() );
	}

	private static SequenceStyleGenerator getSequenceStyleGenerator(org.hibernate.id.NativeGenerator generator)
			throws NoSuchFieldException, IllegalAccessException {
		Field nativeGeneratorField = org.hibernate.id.NativeGenerator.class.getDeclaredField(
				"dialectNativeGenerator" );
		nativeGeneratorField.setAccessible( true );

		Object nativeGenerator = nativeGeneratorField.get( generator );
		assertThat( nativeGenerator ).isInstanceOf( SequenceStyleGenerator.class );

		return (SequenceStyleGenerator) nativeGenerator;
	}

	private static boolean dialectDefaultsToSequenceGenerator(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getDialect()
					.getNativeValueGenerationStrategy() == GenerationType.SEQUENCE;
	}

	@Entity(name = "DefaultIncrSizeDefaultStart")
	@Table(name = "DefaultIncrSizeDefaultStart")
	public static class DefaultIncrSizeDefaultStart {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@NativeGenerator(sequenceForm = @SequenceGenerator())
		public Integer id;
	}

	@Entity(name = "IncrSizeOneStartOne")
	@Table(name = "IncrSizeOneStartOne")
	public static class IncrSizeOneStartOne {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@NativeGenerator(sequenceForm = @SequenceGenerator(initialValue = 1, allocationSize = 1))
		public Integer id;
	}

	@Entity(name = "IncrSizeFiveStartTen")
	@Table(name = "IncrSizeFiveStartTen")
	public static class IncrSizeFiveStartTen {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@NativeGenerator(sequenceForm = @SequenceGenerator(initialValue = 10, allocationSize = 5))
		public Integer id;
	}

}
