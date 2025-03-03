/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.lob;

import org.hibernate.annotations.Immutable;

/**
 * @author Steve Ebersole
 */
@Immutable
public enum PostalArea {
	_78729( "78729", "North Austin", "Austin", State.TX );

	private final String zipCode;
	private final String name;
	private final String cityName;
	private final State state;

	PostalArea(
			String zipCode,
			String name,
			String cityName,
			State state) {
		this.zipCode = zipCode;
		this.name = name;
		this.cityName = cityName;
		this.state = state;
	}

	public static PostalArea fromZipCode(String zipCode) {
		if ( _78729.zipCode.equals( zipCode ) ) {
			return _78729;
		}

		throw new IllegalArgumentException( "Unknown zip code" );
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getName() {
		return name;
	}

	public String getCityName() {
		return cityName;
	}

	public State getState() {
		return state;
	}
}
