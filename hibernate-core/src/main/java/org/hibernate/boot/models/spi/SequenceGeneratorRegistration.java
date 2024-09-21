/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.SequenceGenerator;

/**
 * Global registration of a sequence generator
 *
 * @author Steve Ebersole
 */
public record SequenceGeneratorRegistration(String name, SequenceGenerator configuration) {
}
