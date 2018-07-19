/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.proxy;

import java.io.Serializable;

@SuppressWarnings("all")
public class PersonId implements Serializable {

	private int id;

	private int clientId;

	public PersonId() {
	}

	public PersonId(int aId, int aClientId) {
		setId( aId );
		setClientId( aClientId );
	}

	public int getId() {
		return id;
	}

	public void setId(int aId) {
		this.id = aId;
	}

	public int getClientId() {

		return clientId;
	}

	public void setClientId(int aClientId) {
		clientId = aClientId;
	}
}
