/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.AnnotationUsage;

import jakarta.persistence.SequenceGenerator;

/**
 * Global registration of a sequence generator
 *
 * @author Steve Ebersole
 */
public class SequenceGeneratorRegistration {
	private final String name;
	private final AnnotationUsage<SequenceGenerator> configuration;

	public SequenceGeneratorRegistration(String name, AnnotationUsage<SequenceGenerator> configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public AnnotationUsage<SequenceGenerator> getConfiguration() {
		return configuration;
	}
}
