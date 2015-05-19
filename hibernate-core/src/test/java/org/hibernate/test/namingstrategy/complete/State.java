/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

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
