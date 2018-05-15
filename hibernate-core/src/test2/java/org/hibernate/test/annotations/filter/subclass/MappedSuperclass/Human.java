/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.filter.subclass.MappedSuperclass;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


@Entity
@Table(name="ZOOLOGY_HUMAN")
public class Human extends Mammal {
	@Column(name="HUMAN_IQ")
	private int iq;

	public int getIq() {
		return iq;
	}

	public void setIq(int iq) {
		this.iq = iq;
	}
}
