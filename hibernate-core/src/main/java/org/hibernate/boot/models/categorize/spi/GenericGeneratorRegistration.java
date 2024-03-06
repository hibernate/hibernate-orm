/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.models.spi.AnnotationUsage;

/**
 * Global registration of a generic generator
 *
 * @author Steve Ebersole
 * @see GenericGenerator
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl
 */
public record GenericGeneratorRegistration(String name, AnnotationUsage<GenericGenerator> configuration) {
}
