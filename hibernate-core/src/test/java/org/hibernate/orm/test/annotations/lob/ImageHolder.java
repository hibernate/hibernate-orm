/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;
import java.sql.Types;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * An entity containing data that is materialized into a byte array immediately.
 * The hibernate type mapped for {@link #longByteArray} determines the SQL type
 * asctually used.
 *
 * @author Gail Badner
 */
@Entity
public class ImageHolder {
	private Long id;
	private byte[] longByteArray;
	private Dog dog;
	private Byte[] picByteArray;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@JdbcTypeCode( Types.LONGVARBINARY )
	public byte[] getLongByteArray() {
		return longByteArray;
	}

	public void setLongByteArray(byte[] longByteArray) {
		this.longByteArray = longByteArray;
	}

	@JdbcTypeCode( Types.LONGVARBINARY )
	public Dog getDog() {
		return dog;
	}

	public void setDog(Dog dog) {
		this.dog = dog;
	}

	@JdbcTypeCode( Types.LONGVARBINARY )
	public Byte[] getPicByteArray() {
		return picByteArray;
	}

	public void setPicByteArray(Byte[] picByteArray) {
		this.picByteArray = picByteArray;
	}

}
