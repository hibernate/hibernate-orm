/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.Remove;
import jakarta.persistence.SequenceGenerator;

/**
 * Global registration of a sequence generator
 *
 * @author Steve Ebersole
 */
@Remove
public record SequenceGeneratorRegistration(String name, SequenceGenerator configuration) {
}
