/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.internal;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;

/**
 * A no-op /  pass-through conversion
 *
 * @author Steve Ebersole
 */
public class NoOpBasicValueConverter<O,R> implements BasicValueConverter<O,R> {
	/**
	 * Singleton access
	 */
	public static final NoOpBasicValueConverter INSTANCE = new NoOpBasicValueConverter();

	/**
	 * Typed access to the singleton
	 */
	public static <O,R> NoOpBasicValueConverter<O,R> instance() {
		//noinspection unchecked
		return INSTANCE;
	}

	private NoOpBasicValueConverter() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public O toDomainValue(R relationalForm) {
		return (O) relationalForm;
	}

	@Override
	@SuppressWarnings("unchecked")
	public R toRelationalValue(O domainForm) {
		return (R) domainForm;
	}
}
