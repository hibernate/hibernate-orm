/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.boot.model.Identifier;
import org.hibernate.envers.boot.model.IdentifierRelation;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class IdMappingData {
	private final IdMapper idMapper;
	private final Identifier identifier;
	private final IdentifierRelation relation;

	public IdMappingData(IdMapper mapper, Identifier identifier, IdentifierRelation relation) {
		this.idMapper = mapper;
		this.identifier = identifier;
		this.relation = relation;
	}

	public IdMapper getIdMapper() {
		return idMapper;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public IdentifierRelation getRelation() {
		return relation;
	}
}
