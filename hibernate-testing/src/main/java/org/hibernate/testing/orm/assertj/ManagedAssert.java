/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

public class ManagedAssert<T> extends AbstractObjectAssert<ManagedAssert<T>, Object> {

	public static <T> ManagedAssert<T> assertThatManaged(T managed) {
		return new ManagedAssert<>( managed );
	}

	public static InstanceOfAssertFactory<Object, ManagedAssert<Object>> factory() {
		return new InstanceOfAssertFactory<>( Object.class, ManagedAssert::new );
	}

	public ManagedAssert(Object t) {
		super( t, ManagedAssert.class );
	}

	public ManagedAssert<T> isInitialized(boolean expectInitialized) {
		isNotNull();
		managedInitialization().isEqualTo( expectInitialized );
		return this;
	}

	public ManagedAssert isInitialized() {
		return isInitialized( true );
	}

	@Override
	protected <T> AbstractObjectAssert<?, T> newObjectAssert(T objectUnderTest) {
		return new ManagedAssert( objectUnderTest );
	}

	public ManagedAssert isNotInitialized() {
		return isInitialized( false );
	}

	public ManagedAssert isPropertyInitialized(String propertyName, boolean expectInitialized) {
		isNotNull();
		propertyInitialization( propertyName ).isEqualTo( expectInitialized );
		return this;
	}

	public ManagedAssert isPropertyInitialized(String propertyName) {
		return isPropertyInitialized( propertyName, true );
	}

	public ManagedAssert isPropertyNotInitialized(String propertyName) {
		return isPropertyInitialized( propertyName, false );
	}

	private AbstractBooleanAssert<?> managedInitialization() {
		return assertThat( Hibernate.isInitialized( actual ) )
				.as( "Is '" + actualAsText() + "' initialized?" );
	}

	private AbstractBooleanAssert<?> propertyInitialization(String propertyName) {
		return assertThat( Hibernate.isPropertyInitialized( actual, propertyName ) )
				.as( "Is property '" + propertyName + "' of '" + actualAsText() + "' initialized?" );
	}

	private String actualAsText() {
		String text = descriptionText();
		if ( text == null || text.isEmpty() ) {
			text = String.valueOf( actual );
		}
		return text;
	}

}
