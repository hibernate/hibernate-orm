/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

/**
 * @author Steve Ebersole
 */
public interface Optionable {
	String options();

	void options(String value);
}
