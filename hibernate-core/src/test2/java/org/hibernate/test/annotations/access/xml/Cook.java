/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.test.annotations.access.xml;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Cook {
	@Id
	@GeneratedValue
	private int id;

	private Knive favouriteKnife;

	public Knive getFavouriteKnife() {
		return favouriteKnife;
	}

	public void setFavouriteKnife(Knive favouriteKnife) {
		this.favouriteKnife = favouriteKnife;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
