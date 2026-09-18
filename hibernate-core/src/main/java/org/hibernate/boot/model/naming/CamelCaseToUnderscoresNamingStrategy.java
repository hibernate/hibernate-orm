/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.SPI;

/**
 * @deprecated Use {@link PhysicalNamingStrategySnakeCaseImpl}.
 */
@Deprecated(since = "7", forRemoval = true)
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public class CamelCaseToUnderscoresNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl {
	@SPI(SPI.Role.USE)
	public CamelCaseToUnderscoresNamingStrategy() {
	}

}
