/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.entityNames.singleAssociatedNotAudited;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * @author Hern&aacute;n Chanfreau
 */

public class Car {

	private long id;

	private int number;

	private Person owner;


	public Car() {
	}

	public Car(int number, Person owner) {
		this.number = number;
		this.owner = owner;
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

	@Audited
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}


}
