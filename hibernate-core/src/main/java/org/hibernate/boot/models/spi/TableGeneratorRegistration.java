/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.Remove;
import jakarta.persistence.TableGenerator;

/**
 * Global registration of a table generator
 *
 * @author Steve Ebersole
 */
@Remove
public record TableGeneratorRegistration(String name, TableGenerator configuration) {
}
