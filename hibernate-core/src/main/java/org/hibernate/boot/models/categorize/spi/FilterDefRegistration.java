/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Map;

import org.hibernate.models.spi.ClassDetails;

/**
 * Global registration of a filter definition
 *
 * @see org.hibernate.annotations.FilterDef
 * @see org.hibernate.boot.jaxb.mapping.JaxbFilterDef
 *
 * @author Marco Belladelli
 */
public class FilterDefRegistration {
	private final String name;

	private final String defaultCondition;

	private final Map<String, ClassDetails> parameters;

	public FilterDefRegistration(String name, String defaultCondition, Map<String, ClassDetails> parameters) {
		this.name = name;
		this.defaultCondition = defaultCondition;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public String getDefaultCondition() {
		return defaultCondition;
	}

	public Map<String, ClassDetails> getParameters() {
		return parameters;
	}
}
