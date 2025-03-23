/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.java.LocaleJavaType;

import org.junit.Test;

/**
 * Tests of the {@link LocaleJavaType} class.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class LocaleJavaTypeDescriptorTest extends AbstractDescriptorTest<Locale> {
	final Locale original = toLocale( "de", "DE", null );
	final Locale copy = toLocale( "de", "DE", null );
	final Locale different = toLocale( "de", null, null );

	public LocaleJavaTypeDescriptorTest() {
		super( LocaleJavaType.INSTANCE );
	}

	@Override
	protected Data<Locale> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

	@Override
	protected boolean isIdentityDifferentFromEquality() {
		return false;
	}

	@Test
	public void testConversionFromString() {
		assertEquals( toLocale( "de", null, null ), LocaleJavaType.INSTANCE.fromString( "de" ) );
		assertEquals( toLocale( "de", "DE", null ), LocaleJavaType.INSTANCE.fromString( "de_DE" ) );
		assertEquals( toLocale( null, "DE", null ), LocaleJavaType.INSTANCE.fromString( "_DE" ) );
		assertEquals( toLocale( null, null, "ch123" ), LocaleJavaType.INSTANCE.fromString( "__ch123" ) );
		assertEquals( toLocale( null, "DE", "ch123" ), LocaleJavaType.INSTANCE.fromString( "_DE_ch123" ) );
		assertEquals( toLocale( "de", null, "ch123" ), LocaleJavaType.INSTANCE.fromString( "de__ch123" ) );
		assertEquals( toLocale( "de", "DE", "ch123" ), LocaleJavaType.INSTANCE.fromString( "de_DE_ch123" ) );
		assertEquals( toLocale( "", "", "" ), LocaleJavaType.INSTANCE.fromString( "" ) );
		assertEquals( Locale.ROOT, LocaleJavaType.INSTANCE.fromString( "" ) );
	}

	private static Locale toLocale(String lang, String region, String variant) {
		final Locale.Builder builder = new Locale.Builder();
		if ( StringHelper.isNotEmpty( lang ) ) {
			builder.setLanguage( lang );
		}
		if ( StringHelper.isNotEmpty( region ) ) {
			builder.setRegion( region );
		}
		if ( StringHelper.isNotEmpty( variant ) ) {
			builder.setVariant( variant );
		}
		return builder.build();
	}
}
