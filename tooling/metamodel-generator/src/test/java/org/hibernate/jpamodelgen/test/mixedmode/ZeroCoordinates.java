/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mixedmode;

import javax.persistence.Embeddable;


/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class ZeroCoordinates {
	public float getLatitude() {
		return 0f;
	}

	public void setLatitude(float latitude) {
	}

	public float getLongitude() {
		return 0f;
	}

	public void setLongitude(float longitude) {
	}
}
