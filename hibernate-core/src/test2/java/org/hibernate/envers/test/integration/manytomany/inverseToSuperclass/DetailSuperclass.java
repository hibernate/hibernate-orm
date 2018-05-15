/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany.inverseToSuperclass;

import java.util.List;

import org.hibernate.envers.Audited;

@Audited
public class DetailSuperclass {

	private long id;

	private List<Master> masters;

	public DetailSuperclass() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Master> getMasters() {
		return masters;
	}

	public void setMasters(List<Master> masters) {
		this.masters = masters;
	}

}
