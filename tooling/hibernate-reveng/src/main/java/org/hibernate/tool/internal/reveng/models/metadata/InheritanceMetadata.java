/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.metadata;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

/**
 * Represents metadata for JPA inheritance configuration on a root entity.
 *
 * @author Koen Aers
 */
public class InheritanceMetadata {
	private final InheritanceType strategy;
	private String discriminatorColumnName;
	private DiscriminatorType discriminatorType;
	private int discriminatorColumnLength;

	public InheritanceMetadata(InheritanceType strategy) {
		this.strategy = strategy;
	}

	public InheritanceMetadata discriminatorColumn(String discriminatorColumnName) {
		this.discriminatorColumnName = discriminatorColumnName;
		return this;
	}

	public InheritanceMetadata discriminatorType(DiscriminatorType discriminatorType) {
		this.discriminatorType = discriminatorType;
		return this;
	}

	public InheritanceMetadata discriminatorColumnLength(int discriminatorColumnLength) {
		this.discriminatorColumnLength = discriminatorColumnLength;
		return this;
	}

	// Getters
	public InheritanceType getStrategy() { return strategy; }
	public String getDiscriminatorColumnName() { return discriminatorColumnName; }
	public DiscriminatorType getDiscriminatorType() { return discriminatorType; }
	public int getDiscriminatorColumnLength() { return discriminatorColumnLength; }
}
