/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.attributeoverride;

import jakarta.persistence.Embeddable;

@Embeddable
public class Route {

	private String origin;
	private String destination;

	public Route() {
	}

	public Route(String origin, String destination) {
		this.origin = origin;
		this.destination = destination;
	}
}
