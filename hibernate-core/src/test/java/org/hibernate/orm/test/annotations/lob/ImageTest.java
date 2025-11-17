/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.type.WrapperArrayHandling;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;


/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.StandardBasicTypes#IMAGE}.
 *
 * @author Gail Badner
 */
@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(SybaseDialect.class)
@DomainModel(
		annotatedClasses = {
				ImageHolder.class
		},
		annotatedPackageNames = "org.hibernate.orm.test.annotations.lob"
)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.WRAPPER_ARRAY_HANDLING,
				provider = ImageTest.WrapperArrayHandlingProvider.class)
)
public class ImageTest {
	private static final int ARRAY_SIZE = 10000;

	public static class WrapperArrayHandlingProvider implements SettingProvider.Provider<WrapperArrayHandling> {
		@Override
		public WrapperArrayHandling getSetting() {
			return WrapperArrayHandling.ALLOW;
		}
	}

	@Test
	public void testBoundedLongByteArrayAccess(SessionFactoryScope scope) {
		byte[] original = buildRecursively( ARRAY_SIZE, true );
		byte[] changed = buildRecursively( ARRAY_SIZE, false );

		ImageHolder e = new ImageHolder();
		scope.inTransaction(
				session ->
						session.persist( e )
		);

		Dog dog = new Dog();
		scope.inTransaction(
				session -> {
					ImageHolder entity = session.get( ImageHolder.class, e.getId() );
					assertThat( entity.getLongByteArray() ).isNull();
					assertThat( entity.getDog() ).isNull();
					assertThat( entity.getPicByteArray() ).isNull();
					entity.setLongByteArray( original );

					dog.setName( "rabbit" );
					entity.setDog( dog );
					entity.setPicByteArray( wrapPrimitive( original ) );
				}
		);

		scope.inTransaction(
				session -> {
					ImageHolder entity = session.find( ImageHolder.class, e.getId() );
					assertThat( entity.getLongByteArray().length ).isEqualTo( ARRAY_SIZE );
					assertEquals( original, entity.getLongByteArray() );
					assertThat( entity.getPicByteArray().length ).isEqualTo( ARRAY_SIZE );
					assertEquals( original, unwrapNonPrimitive( entity.getPicByteArray() ) );
					assertThat( entity.getDog() ).isNotNull();
					assertThat( entity.getDog().getName() ).isEqualTo( dog.getName() );
					entity.setLongByteArray( changed );
					entity.setPicByteArray( wrapPrimitive( changed ) );
					dog.setName( "papa" );
					entity.setDog( dog );
				}
		);

		scope.inTransaction(
				session -> {
					ImageHolder entity =
							session.find( ImageHolder.class, e.getId() );
					assertThat( entity.getLongByteArray().length ).isEqualTo( ARRAY_SIZE );
					assertEquals( changed, entity.getLongByteArray() );
					assertThat( entity.getPicByteArray().length ).isEqualTo( ARRAY_SIZE );
					assertEquals( changed, unwrapNonPrimitive( entity.getPicByteArray() ) );
					assertThat( entity.getDog() ).isNotNull();
					assertThat( entity.getDog().getName() ).isEqualTo( dog.getName() );
					entity.setLongByteArray( null );
					entity.setPicByteArray( null );
					entity.setDog( null );
				}
		);

		scope.inTransaction(
				session -> {
					ImageHolder entity = session.find( ImageHolder.class, e.getId() );
					assertThat( entity.getLongByteArray() ).isNull();
					assertThat( entity.getDog() ).isNull();
					assertThat( entity.getPicByteArray() ).isNull();
					session.remove( entity );
				}
		);
	}

	private Byte[] wrapPrimitive(byte[] bytes) {
		int length = bytes.length;
		Byte[] result = new Byte[length];
		for ( int index = 0; index < length; index++ ) {
			result[index] = bytes[index];
		}
		return result;
	}

	private byte[] unwrapNonPrimitive(Byte[] bytes) {
		int length = bytes.length;
		byte[] result = new byte[length];
		for ( int i = 0; i < length; i++ ) {
			result[i] = bytes[i];
		}
		return result;
	}

	private byte[] buildRecursively(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask( on );
		for ( int i = 0; i < size; i++ ) {
			data[i] = mask( on );
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? (byte) 1 : (byte) 0;
	}

	public static void assertEquals(byte[] val1, byte[] val2) {
		if ( !Arrays.equals( val1, val2 ) ) {
			fail( "byte arrays did not match" );
		}
	}

}
