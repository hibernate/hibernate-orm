/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Manuel Bernhardt
 */
@MappedSuperclass
public class Building {

	public Long height;

	private Room room;

	public Long getHeight() {
		return height;
	}

	public void setHeight(Long height) {
		this.height = height;
	}

	@ManyToOne(optional = false)
	public Room getRoom() {
		return room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}
}
