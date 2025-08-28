/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RelationDescription {
	private final String fromPropertyName;
	private final RelationType relationType;
	private final String toEntityName;
	private final String mappedByPropertyName;
	private final boolean ignoreNotFound;
	private final IdMapper idMapper;
	private final PropertyMapper fakeBidirectionalRelationMapper;
	private final PropertyMapper fakeBidirectionalRelationIndexMapper;
	private final MiddleIdData referencingIdData;
	private final MiddleIdData referencedIdData;
	private final String auditMiddleEntityName;
	private final boolean insertable;
	private final boolean indexed;
	private boolean bidirectional;

	public static RelationDescription toOne(
			String fromPropertyName,
			RelationType relationType,
			String toEntityName,
			String mappedByPropertyName,
			IdMapper idMapper,
			PropertyMapper fakeBidirectionalRelationMapper,
			PropertyMapper fakeBidirectionalRelationIndexMapper,
			boolean insertable,
			boolean ignoreNotFound) {
		return new RelationDescription(
				fromPropertyName, relationType, toEntityName, mappedByPropertyName, idMapper,
				fakeBidirectionalRelationMapper, fakeBidirectionalRelationIndexMapper, null, null, null, insertable, ignoreNotFound, false
		);
	}

	public static RelationDescription toMany(
			String fromPropertyName,
			RelationType relationType,
			String toEntityName,
			String mappedByPropertyName,
			IdMapper idMapper,
			PropertyMapper fakeBidirectionalRelationMapper,
			PropertyMapper fakeBidirectionalRelationIndexMapper,
			MiddleIdData referencingIdData,
			MiddleIdData referencedIdData,
			String auditMiddleEntityName,
			boolean insertable,
			boolean indexed) {
		// Envers populates collections by executing dedicated queries. Special handling of
		// @NotFound(action = NotFoundAction.IGNORE) can be omitted in such case as exceptions
		// (e.g. EntityNotFoundException, ObjectNotFoundException) are never thrown.
		// Therefore assigning false to ignoreNotFound.
		return new RelationDescription(
				fromPropertyName, relationType, toEntityName, mappedByPropertyName, idMapper, fakeBidirectionalRelationMapper,
				fakeBidirectionalRelationIndexMapper, referencingIdData, referencedIdData, auditMiddleEntityName, insertable, false, indexed
		);
	}

	private RelationDescription(
			String fromPropertyName,
			RelationType relationType,
			String toEntityName,
			String mappedByPropertyName,
			IdMapper idMapper,
			PropertyMapper fakeBidirectionalRelationMapper,
			PropertyMapper fakeBidirectionalRelationIndexMapper,
			MiddleIdData referencingIdData,
			MiddleIdData referencedIdData,
			String auditMiddleEntityName,
			boolean insertable,
			boolean ignoreNotFound,
			boolean indexed) {
		this.fromPropertyName = fromPropertyName;
		this.relationType = relationType;
		this.toEntityName = toEntityName;
		this.mappedByPropertyName = mappedByPropertyName;
		this.ignoreNotFound = ignoreNotFound;
		this.idMapper = idMapper;
		this.fakeBidirectionalRelationMapper = fakeBidirectionalRelationMapper;
		this.fakeBidirectionalRelationIndexMapper = fakeBidirectionalRelationIndexMapper;
		this.referencingIdData = referencingIdData;
		this.referencedIdData = referencedIdData;
		this.auditMiddleEntityName = auditMiddleEntityName;
		this.insertable = insertable;
		this.indexed = indexed;
		this.bidirectional = false;
	}

	public String getFromPropertyName() {
		return fromPropertyName;
	}

	public RelationType getRelationType() {
		return relationType;
	}

	public String getToEntityName() {
		return toEntityName;
	}

	public String getMappedByPropertyName() {
		return mappedByPropertyName;
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public IdMapper getIdMapper() {
		return idMapper;
	}

	public PropertyMapper getFakeBidirectionalRelationMapper() {
		return fakeBidirectionalRelationMapper;
	}

	public PropertyMapper getFakeBidirectionalRelationIndexMapper() {
		return fakeBidirectionalRelationIndexMapper;
	}

	public MiddleIdData getReferencingIdData() {
		return referencingIdData;
	}

	public MiddleIdData getReferencedIdData() {
		return referencedIdData;
	}

	public String getAuditMiddleEntityName() {
		return auditMiddleEntityName;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public boolean isIndexed() {
		return indexed;
	}

	public boolean isBidirectional() {
		return bidirectional;
	}

	void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}
}
