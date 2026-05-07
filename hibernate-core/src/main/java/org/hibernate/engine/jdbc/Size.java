/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import org.hibernate.Length;

import java.io.Serializable;

/**
 * Models size restrictions/requirements on a column's data type.
 *
 * @implNote Since we do not necessarily know the data type up front,
 * and therefore do not necessarily know whether length or
 * precision/scale sizing is needed, we simply account for both here.
 *
 * @author Steve Ebersole
 */
public class Size implements Serializable {

	public static final long DEFAULT_LENGTH = Length.DEFAULT;
	public static final long LONG_LENGTH = Length.LONG;
	public static final long DEFAULT_LOB_LENGTH = Length.LOB_DEFAULT;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;

	private Integer precision;
	private Integer scale;

	private Long length;
	private Integer arrayLength;

	public Size() {
	}

	public Size(Integer precision, Integer scale, Long length) {
		this.precision = precision;
		this.scale = scale;
		this.length = length;
	}

	public static Size nil() {
		return new Size();
	}

	public static Size precision(int precision) {
		return new Size( precision, -1, -1L );
	}

	public static Size precision(int precision, int scale) {
		return new Size( precision, scale, -1L );
	}

	public static Size length(long length) {
		return new Size( -1, -1, length );
	}

	public Integer getPrecision() {
		return precision;
	}

	public Integer getScale() {
		return scale;
	}

	public Long getLength() {
		return length;
	}

	public Integer getArrayLength() {
		return arrayLength;
	}

	public void initialize(Size size) {
		this.precision = size.precision;
		this.scale =  size.scale;
		this.length = size.length;
	}

	public Size setPrecision(Integer precision) {
		this.precision = precision;
		return this;
	}

	public Size setScale(Integer scale) {
		this.scale = scale;
		return this;
	}

	public Size setLength(Long length) {
		this.length = length;
		return this;
	}

	public Size setArrayLength(Integer arrayLength) {
		this.arrayLength = arrayLength;
		return this;
	}
}
