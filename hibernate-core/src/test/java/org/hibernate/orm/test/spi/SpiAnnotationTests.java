/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.spi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.EnumSet;

import org.hibernate.SPI;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the declaration-level contract of [SPI].
///
/// @author Steve Ebersole
public class SpiAnnotationTests {
	private static final DotName SPI_NAME = DotName.createSimple( SPI.class.getName() );

	@Test
	public void annotationDeclaration() throws NoSuchMethodException {
		assertEquals( RetentionPolicy.RUNTIME, SPI.class.getAnnotation( Retention.class ).value() );
		assertEquals(
				EnumSet.of(
						ElementType.PACKAGE,
						ElementType.TYPE,
						ElementType.METHOD,
						ElementType.FIELD,
						ElementType.CONSTRUCTOR,
						ElementType.ANNOTATION_TYPE
				),
				EnumSet.copyOf( java.util.List.of( SPI.class.getAnnotation( Target.class ).value() ) )
		);
		assertTrue( SPI.class.isAnnotationPresent( Documented.class ) );
		assertFalse( SPI.class.isAnnotationPresent( Inherited.class ) );
		assertArrayEquals(
				new SPI.Role[] { USE },
				(SPI.Role[]) SPI.class.getDeclaredMethod( "value" ).getDefaultValue()
		);
	}

	@Test
	public void supportedTargets() throws NoSuchMethodException, NoSuchFieldException {
		assertRoles( SpiAnnotationTests.class.getPackage(), USE );
		assertRoles( TargetFixture.class, USE );
		assertRoles( TargetFixture.class.getDeclaredConstructor(), IMPLEMENT );
		assertRoles( TargetFixture.class.getDeclaredMethod( "supply" ), SUPPLY );
		assertRoles( TargetFixture.class.getDeclaredField( "value" ), USE, SUPPLY );
		assertRoles( AnnotationTargetFixture.class, IMPLEMENT, SUPPLY );
	}

	@Test
	public void independentRoleCombinations() {
		assertRoles( UseOnly.class, USE );
		assertRoles( ImplementOnly.class, IMPLEMENT );
		assertRoles( SupplyOnly.class, SUPPLY );
		assertRoles( UseImplement.class, USE, IMPLEMENT );
		assertRoles( UseSupply.class, USE, SUPPLY );
		assertRoles( ImplementSupply.class, IMPLEMENT, SUPPLY );
		assertRoles( UseImplementSupply.class, USE, IMPLEMENT, SUPPLY );
	}

	@Test
	public void explicitlyEmptyRolesAreObservableForValidation() {
		assertRoles( EmptyRoles.class );
	}

	@Test
	public void rolesArePreservedByJandex() throws IOException {
		final Indexer indexer = new Indexer();
		try ( InputStream stream = classFile( UseImplementSupply.class ) ) {
			indexer.index( stream );
		}
		final ClassInfo classInfo = indexer.complete().getClassByName(
				DotName.createSimple( UseImplementSupply.class.getName() )
		);

		final AnnotationInstance annotation = classInfo.declaredAnnotation( SPI_NAME );
		assertNotNull( annotation );
		assertArrayEquals(
				new String[] { "USE", "IMPLEMENT", "SUPPLY" },
				annotation.value().asEnumArray()
		);
	}

	private static InputStream classFile(Class<?> type) {
		final String resourceName = "/" + type.getName().replace( '.', '/' ) + ".class";
		final InputStream stream = type.getResourceAsStream( resourceName );
		assertNotNull( stream, resourceName );
		return stream;
	}

	private static void assertRoles(AnnotatedElement element, SPI.Role... expectedRoles) {
		final SPI annotation = element.getAnnotation( SPI.class );
		assertNotNull( annotation, element.toString() );
		assertArrayEquals( expectedRoles, annotation.value(), element.toString() );
	}

	@SPI
	private static class TargetFixture {
		@SPI({ USE, SUPPLY })
		private String value;

		@SPI(IMPLEMENT)
		private TargetFixture() {
		}

		@SPI(SUPPLY)
		private void supply() {
		}
	}

	@SPI({ IMPLEMENT, SUPPLY })
	private @interface AnnotationTargetFixture {
	}

	@SPI
	private static class UseOnly {
	}

	@SPI(IMPLEMENT)
	private static class ImplementOnly {
	}

	@SPI(SUPPLY)
	private static class SupplyOnly {
	}

	@SPI({ USE, IMPLEMENT })
	private static class UseImplement {
	}

	@SPI({ USE, SUPPLY })
	private static class UseSupply {
	}

	@SPI({ IMPLEMENT, SUPPLY })
	private static class ImplementSupply {
	}

	@SPI({ USE, IMPLEMENT, SUPPLY })
	private static class UseImplementSupply {
	}

	@SPI({})
	private static class EmptyRoles {
	}
}
