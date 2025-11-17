/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import java.sql.Types;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * An entity containing data that is materialized into a String immediately.
 * The hibernate type mapped for {@link #LONGVARCHAR} determines the SQL type
 * actually used.
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

	@JdbcTypeCode( Types.LONGVARCHAR )
	public String getLongString() {
		return longString;
	}

	public void setLongString(String longString) {
		this.longString = longString;
	}

	@JdbcTypeCode( Types.LONGVARCHAR )
	public char[] getName() {
		return name;
	}

	public void setName(char[] name) {
		this.name = name;
	}

	@JdbcTypeCode( Types.LONGVARCHAR )
	@JavaType( CharacterArrayJavaType.class )
	public Character[] getWhatEver() {
		return whatEver;
	}

	public void setWhatEver(Character[] whatEver) {
		this.whatEver = whatEver;
	}
}
