/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.IdMappingData;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;

/**
 * A class holding information about ids, which form a virtual "relation" from a middle-table. Middle-tables are used
 * when mapping collections.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class MiddleIdData {
	private final IdMapper originalMapper;
	private final IdMapper prefixedMapper;
	private final String entityName;
	private final String auditEntityName;

	public MiddleIdData(
			Configuration configuration,
			IdMappingData mappingData,
			String prefix,
			String entityName,
			boolean audited) {
		this.originalMapper = mappingData.getIdMapper();
		this.prefixedMapper = mappingData.getIdMapper().prefixMappedProperties( prefix );
		this.entityName = entityName;
		this.auditEntityName = audited ? configuration.getAuditEntityName( entityName ) : null;
	}

	/**
	 * @return Original id mapper of the related entity.
	 */
	public IdMapper getOriginalMapper() {
		return originalMapper;
	}

	/**
	 * @return prefixed id mapper (with the names for the id fields that are used in the middle table) of the related entity.
	 */
	public IdMapper getPrefixedMapper() {
		return prefixedMapper;
	}

	/**
	 * @return Name of the related entity (regular, not audited).
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * @return Audit name of the related entity.
	 */
	public String getAuditEntityName() {
		return auditEntityName;
	}

	/**
	 * @return Is the entity, to which this middle id data correspond, audited.
	 */
	public boolean isAudited() {
		return auditEntityName != null;
	}
}
