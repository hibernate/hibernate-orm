/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.SizeSource;

/**
 * Implementation of SizeSource
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class SizeSourceImpl implements SizeSource {
	private final Integer length;
	private final Integer scale;
	private final Integer precision;

	public SizeSourceImpl(Integer length, Integer scale, Integer precision) {
		this.length = length;
		this.scale = scale;
		this.precision = precision;
	}

	@Override
	public Integer getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getScale() {
		return scale;
	}
}
