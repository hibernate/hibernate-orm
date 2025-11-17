/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * @author Steve Ebersole
 */
public interface SizeSource {
	/**
	 * The specified length.  Will return {@code null} if none was specified.
	 *
	 * @return The length, or {@code null} if not defined.
	 */
	Integer getLength();

	/**
	 * The specified precision.  Will return {@code null} if none was specified.
	 *
	 * @return The precision, or {@code null} if not defined.
	 */
	Integer getPrecision();

	/**
	 * The specified scale.  Will return {@code null} if none was specified.
	 *
	 * @return The scale, or {@code null} if not defined.
	 */
	Integer getScale();
}
