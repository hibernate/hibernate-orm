/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.net.URL;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypeTemplate;
import org.hibernate.type.descriptor.java.UrlJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Test making sure StandardBasicTypeTemplate works
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
public class StandardBasicTypeTemplateTest {

	public static final String REG_KEY = "validating-url";

	@Test
	public void testContributedBasicType() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		typeConfiguration.getJavaTypeRegistry().addDescriptor( ValidatingUrlJavaJavaType.INSTANCE );
		typeConfiguration.getBasicTypeRegistry().register(
				new StandardBasicTypeTemplate<>(
						VarcharJdbcType.INSTANCE,
						ValidatingUrlJavaJavaType.INSTANCE,
						REG_KEY
				)
		);

		final BasicType registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( REG_KEY );
		assertThat( registeredType, notNullValue() );
		assertTyping( StandardBasicTypeTemplate.class, registeredType );
	}

	private static class ValidatingUrlJavaJavaType extends UrlJavaType {
		/**
		 * Singleton access
		 */
		public static final ValidatingUrlJavaJavaType INSTANCE = new ValidatingUrlJavaJavaType();

		@Override
		public URL fromString(CharSequence string) {
			if ( "invalid".equals( string.toString() ) ) {
				throw new IllegalStateException( "Invalid url" );
			}
			return super.fromString( string );
		}
	}
}
