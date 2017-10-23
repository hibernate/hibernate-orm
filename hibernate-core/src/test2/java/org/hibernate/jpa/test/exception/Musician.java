/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.jpa.test.exception;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Hardy Ferentschik
 */
@Entity
@SuppressWarnings("serial")
public class Musician implements Serializable {
	private Integer id;
	
	private String name;
	
	private Music favouriteMusic;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public Music getFavouriteMusic() {
		return favouriteMusic;
	}

	public void setFavouriteMusic(Music favouriteMusic) {
		this.favouriteMusic = favouriteMusic;
	}
}
