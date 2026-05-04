/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

/**
 * Represents metadata for JPA inheritance configuration on a root entity.
 *
 * @author Koen Aers
 */
public class InheritanceDescriptor {
	private final InheritanceType strategy;
	private String discriminatorColumnName;
	private DiscriminatorType discriminatorType;
	private int discriminatorColumnLength;

	public InheritanceDescriptor(InheritanceType strategy) {
		this.strategy = strategy;
	}

	public InheritanceDescriptor discriminatorColumn(String discriminatorColumnName) {
		this.discriminatorColumnName = discriminatorColumnName;
		return this;
	}

	public InheritanceDescriptor discriminatorType(DiscriminatorType discriminatorType) {
		this.discriminatorType = discriminatorType;
		return this;
	}

	public InheritanceDescriptor discriminatorColumnLength(int discriminatorColumnLength) {
		this.discriminatorColumnLength = discriminatorColumnLength;
		return this;
	}

	// Getters
	public InheritanceType getStrategy() { return strategy; }
	public String getDiscriminatorColumnName() { return discriminatorColumnName; }
	public DiscriminatorType getDiscriminatorType() { return discriminatorType; }
	public int getDiscriminatorColumnLength() { return discriminatorColumnLength; }
}
