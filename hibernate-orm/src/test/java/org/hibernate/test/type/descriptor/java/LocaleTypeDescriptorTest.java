/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.descriptor.java.LocaleTypeDescriptor;
import org.junit.Test;

/**
 * Tests of the {@link LocaleTypeDescriptor} class.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public class LocaleTypeDescriptorTest extends BaseUnitTestCase {

	@Test
	public void testConversionFromString() {
		assertEquals( toLocale( "de", null, null ), LocaleTypeDescriptor.INSTANCE.fromString( "de" ) );
		assertEquals( toLocale( "de", "DE", null ), LocaleTypeDescriptor.INSTANCE.fromString( "de_DE" ) );
		assertEquals( toLocale( null, "DE", null ), LocaleTypeDescriptor.INSTANCE.fromString( "_DE" ) );
		assertEquals( toLocale( null, null, "ch123" ), LocaleTypeDescriptor.INSTANCE.fromString( "__ch123" ) );
		assertEquals( toLocale( null, "DE", "ch123" ), LocaleTypeDescriptor.INSTANCE.fromString( "_DE_ch123" ) );
		assertEquals( toLocale( "de", null, "ch123" ), LocaleTypeDescriptor.INSTANCE.fromString( "de__ch123" ) );
		assertEquals( toLocale( "de", "DE", "ch123" ), LocaleTypeDescriptor.INSTANCE.fromString( "de_DE_ch123" ) );
		assertEquals( toLocale( "", "", "" ), LocaleTypeDescriptor.INSTANCE.fromString( "" ) );
		assertEquals( Locale.ROOT, LocaleTypeDescriptor.INSTANCE.fromString( "" ) );
	}

	public Locale toLocale(String lang, String region, String variant) {
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
