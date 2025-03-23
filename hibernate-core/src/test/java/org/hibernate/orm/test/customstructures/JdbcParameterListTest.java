/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.customstructures;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for JdbcParametersList
 */
public class JdbcParameterListTest {

	@Test
	public void emptyConstant() {
		final JdbcParametersList empty = JdbcParametersList.empty();
		expectsEmpty( empty );
	}

	@Test
	public void singleton() {
		final JdbcParameterImpl element = makeJdbcParameterElement();
		final JdbcParametersList singleton = JdbcParametersList.singleton( element );
		expectsSize( 1, singleton );
		Assert.assertSame( element, singleton.get( 0 ) );
	}

	@Test
	public void emptyBuilderDefault() {
		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		expectsEmpty( builder.build() );
	}

	@Test
	public void emptyBuilderSized() {
		for ( int i = 0; i < 5; i++ ) {
			final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder( i );
			expectsEmpty( builder.build() );
		}
	}

	@Test
	public void singletonBuilderDefault() {
		final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
		verifyAsSingletonBuilder( builder );
	}

	private void verifyAsSingletonBuilder(JdbcParametersList.Builder builder) {
		final JdbcParameterImpl element = makeJdbcParameterElement();
		builder.add( element );
		final JdbcParametersList built = builder.build();
		expectsSize( 1, built );
		Assert.assertSame( element, built.get( 0 ) );
	}

	@Test
	public void singletonBuilderSized() {
		for ( int i = 0; i < 5; i++ ) {
			final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder( i );
			verifyAsSingletonBuilder( builder );
		}
	}

	@Test
	public void multiBuilderDefault() {
		for ( int size = 0; size < 15; size++ ) {
			final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder();
			verifyNparamBuilder( size, builder );
		}
	}

	@Test
	public void multiBuilderSized() {
		for ( int hintSize = 0; hintSize < 5; hintSize++ ) {
			for ( int size = 0; size < 15; size++ ) {
				final JdbcParametersList.Builder builder = JdbcParametersList.newBuilder( hintSize );
				verifyNparamBuilder( size, builder );
			}
		}
	}

	private void verifyNparamBuilder(final int size, final JdbcParametersList.Builder builder) {
		final JdbcParameterImpl[] elements = new JdbcParameterImpl[size];
		for ( int i = 0; i < size; i++ ) {
			elements[i] = makeJdbcParameterElement();
		}
		for ( JdbcParameter element : elements ) {
			builder.add( element );
		}
		final JdbcParametersList built = builder.build();
		expectsSize( size, built );
		for ( int i = 0; i < size; i++ ) {
			Assert.assertSame( elements[i], built.get( i ) );
		}
	}

	private static void expectsEmpty(JdbcParametersList empty) {
		expectsSize( 0, empty );
	}

	private static void expectsSize(int size, JdbcParametersList list) {
		Assert.assertEquals( size, list.size() );
		for ( int i = 0; i < size; i++ ) {
			Assert.assertNotNull( list.get( i ) );
		}
		if ( size == 0 ) {
			Assert.assertSame( JdbcParametersList.empty(), list );
		}
		else if ( size == 1 ) {
			Assert.assertTrue( list instanceof JdbcParametersList.JdbcParametersListSingleton );
		}
		Assert.assertThrows( ArrayIndexOutOfBoundsException.class, () -> list.get( size ) );
	}

	private static JdbcParameterImpl makeJdbcParameterElement() {
		return new JdbcParameterImpl( null );
	}

}
