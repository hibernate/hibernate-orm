/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
