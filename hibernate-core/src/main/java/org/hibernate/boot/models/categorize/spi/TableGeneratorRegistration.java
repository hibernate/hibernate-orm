/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.TableGenerator;

/**
 * Global registration of a table generator
 *
 * @author Steve Ebersole
 */
public record TableGeneratorRegistration(String name, AnnotationUsage<TableGenerator> configuration) {
}
