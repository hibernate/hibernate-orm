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
 * @see GenericGenerator
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl
 *
 * @author Steve Ebersole
 */
public class GenericGeneratorRegistration {
	private final String name;
	private final AnnotationUsage<GenericGenerator> configuration;

	public GenericGeneratorRegistration(String name, AnnotationUsage<GenericGenerator> configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public AnnotationUsage<GenericGenerator> getConfiguration() {
		return configuration;
	}
}
