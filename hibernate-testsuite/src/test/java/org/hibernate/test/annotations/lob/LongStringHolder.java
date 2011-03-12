//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
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
