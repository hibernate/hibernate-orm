/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.annotations.lob;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

/**
 * An entity containing data that is materialized into a String immediately.
 * The hibernate type mapped for {@link #LONGVARCHAR} determines the SQL type
 * asctually used.
 * 
 * @author Gail Badner
 */
@Entity
public class LongStringHolder {
	private Long id;
	private char[] name;
	private Character[] whatEver;
	private String longString;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Type(type = "text")
	public String getLongString() {
		return longString;
	}

	public void setLongString(String longString) {
		this.longString = longString;
	}
	@Type(type = "char_text")
	public char[] getName() {
		return name;
	}

	public void setName(char[] name) {
		this.name = name;
	}
	@Type(type = "wrapped_char_text")
	public Character[] getWhatEver() {
		return whatEver;
	}

	public void setWhatEver(Character[] whatEver) {
		this.whatEver = whatEver;
	}
}
