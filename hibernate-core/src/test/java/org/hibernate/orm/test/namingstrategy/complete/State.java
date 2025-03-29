/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.complete;

/**
 * @author Steve Ebersole
 */
public enum State {
	ALABAMA( "AL", "Alabama" ),
	ALASKA( "AK", "Alaska" ),
	ARIZONA( "AZ", "Arizona" ),
	ARKANSAS( "AR", "Arkansas" )
	// etc
	;

	private final String isoCode;
	private final String text;

	private State(String isoCode, String text) {
		this.isoCode = isoCode;
		this.text = text;
	}

}
