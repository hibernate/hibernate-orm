/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import static org.hibernate.internal.util.StringHelper.isEmpty;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.java.LocaleJavaType;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the {@link LocaleJavaType} class.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class LocaleJavaTypeDescriptorTest extends AbstractDescriptorTest<Locale> {
	final Locale original = toLocale( "de", "DE", null, null );
	final Locale copy = toLocale( "de", "DE", null, null );
	final Locale different = toLocale( "de", null, null, null );

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
		assertLocaleString( toLocale( "de", null, null, null ), "de", "de" );
		assertLocaleString( toLocale( "de", "DE", null, null ), "de_DE", "de-DE" );
		assertLocaleString( toLocale( null, "DE", null, null ), "_DE", "und-DE" );
		assertLocaleString( toLocale( null, "DE", "ch123", null ), "_DE_ch123", "und-DE-ch123" );
		assertLocaleString( toLocale( "de", null, "ch123", null ), "de__ch123", "de-ch123" );
		assertLocaleString( toLocale( "de", "DE", "ch123", null ), "de_DE_ch123", "de-DE-ch123" );
		assertLocaleString( toLocale( "zh", "HK", null, "Hant" ), "zh_HK_#Hant", "zh-Hant-HK" );
		assertLocaleString( toLocale( "ja", null, null, null, "u-nu-japanese" ), "ja__#u-nu-japanese", "ja-u-nu-japanese" );
		assertLocaleString( toLocale( "ja", null, null, null, "u-nu-japanese", "x-linux" ), "ja__#u-nu-japanese-x-linux", "ja-u-nu-japanese-x-linux" );
		assertLocaleString( toLocale( "ja", "JP", null, null, "u-nu-japanese" ), "ja_JP_#u-nu-japanese", "ja-JP-u-nu-japanese" );
		assertLocaleString( toLocale( "ja", "JP", null, null, "u-nu-japanese", "x-linux" ), "ja_JP_#u-nu-japanese-x-linux", "ja-JP-u-nu-japanese-x-linux" );
		assertLocaleString( toLocale( "ja", "JP", null, "Jpan", "u-nu-japanese" ), "ja_JP_#Jpan_u-nu-japanese", "ja-Jpan-JP-u-nu-japanese" );
		assertLocaleString( toLocale( "ja", "JP", null, "Jpan", "u-nu-japanese", "x-linux" ), "ja_JP_#Jpan_u-nu-japanese-x-linux", "ja-Jpan-JP-u-nu-japanese-x-linux" );
		// Note that these Locale objects make no sense, since Locale#toString requires at least a language or region
		// to produce a non-empty string, but we test parsing that anyway, especially since the language tag now produces a value
		assertLocaleString( toLocale( null, null, "ch123", null ), "__ch123", "und-ch123" );
		assertLocaleString( toLocale( "", "", "", null ), "", "und" );
		assertLocaleString( toLocale( null, null, null, "Hant" ), "___#Hant", "und-Hant" );
		assertLocaleString( Locale.ROOT, "", "und" );
	}

	private void assertLocaleString(Locale expectedLocale, String string, String languageTag) {
		assertEquals( expectedLocale, LocaleJavaType.INSTANCE.fromString( string ) );
		assertEquals( expectedLocale, LocaleJavaType.INSTANCE.fromString( languageTag ) );
		assertEquals( expectedLocale.toLanguageTag(), languageTag );
		if ( !isEmpty( expectedLocale.getLanguage() ) || !isEmpty( expectedLocale.getCountry() ) ) {
			assertEquals( expectedLocale.toString(), string );
		}
	}

	private static Locale toLocale(String lang, String region, String variant, String script, String... extensions) {
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
		if ( StringHelper.isNotEmpty( script ) ) {
			builder.setScript( script );
		}
		for ( String extension : extensions ) {
			assert extension.charAt( 1 ) == '-';
			builder.setExtension( extension.charAt( 0 ), extension.substring( 2 ) );
		}
		return builder.build();
	}
}
