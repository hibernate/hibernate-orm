package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.bytecode.spi.Generated;
import org.hibernate.testing.bytecode.enhancement.EnhancementTestConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class GeneratedEnhancementTest {

	private static final String PREFIX = "$$_hibernate_";

	@ParameterizedTest
	@ValueSource(classes = TestEntity.class)
	void generatedAnnotationIsPresent(Class<?> testClass) {
		final var typeDescription = enhanceAndDescribe( testClass );

		for (MethodDescription.InDefinedShape method : typeDescription.getDeclaredMethods()) {
			assertGeneratedAnnotation( method );
		}

		typeDescription.getDeclaredMethods().stream()
				.filter( method -> method.getName().startsWith( PREFIX ) )
				.forEach( GeneratedEnhancementTest::assertGeneratedAnnotation );
	}

	private static void assertGeneratedAnnotation(MethodDescription.InDefinedShape method) {
		assertThat( method.getDeclaredAnnotations().isAnnotationPresent( Generated.class ) )
				.isEqualTo( method.getName().startsWith( PREFIX ) );
	}

	private static TypeDescription enhanceAndDescribe(Class<?> clazz) {
		final Enhancer enhancer = EnhancementTestConfiguration.createEnhancer( new EnhancementTestConfiguration(), new ByteBuddyState() );
		final byte[] enhancedBytes = enhance( clazz, enhancer );

		try (final var enhancedClassLocator = ClassFileLocator.Simple.of( clazz.getName(), enhancedBytes );
			final var classLoaderLocator = ClassFileLocator.ForClassLoader.of( GeneratedEnhancementTest.class.getClassLoader() );
			final var locator = new ClassFileLocator.Compound( enhancedClassLocator, classLoaderLocator )) {
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
	static class TestEntity {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

}
