/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.strategy.spi;

import org.hibernate.Incubating;
import org.hibernate.envers.boot.model.PersistentEntity;
import org.hibernate.envers.configuration.Configuration;


/**
 * Describes an audit mapping context.
 *
 * @author Chris Cranford
 */
@Incubating
public class MappingContext {
	private final PersistentEntity mapping;
	private final Configuration configuration;
	private final String revisionInfoPropertyType;
	private final String revisionInfoExplicitTypeName;
	private final boolean revisionEndTimestampOnly;
	public MappingContext(
			PersistentEntity mapping,
			Configuration configuration,
			String revisionInfoPropertyType,
			String revisionInfoExplicitTypeName,
			boolean revisionEndTimestampOnly) {
		this.mapping = mapping;
		this.configuration = configuration;
		this.revisionInfoPropertyType = revisionInfoPropertyType;
		this.revisionInfoExplicitTypeName = revisionInfoExplicitTypeName;
		this.revisionEndTimestampOnly = revisionEndTimestampOnly;
	}

	public PersistentEntity getEntityMapping() {
		return mapping;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public String getRevisionInfoPropertyType() {
		return revisionInfoPropertyType;
	}

	public String getRevisionInfoExplicitTypeName() {
		return revisionInfoExplicitTypeName;
	}

	public boolean isRevisionEndTimestampOnly() {
		return revisionEndTimestampOnly;
	}
}
