/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;

/**
 * @author Adam Warski (adam at warski dot org)
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
	private final boolean insertable;
	private boolean bidirectional;

	public static RelationDescription toOne(String fromPropertyName, RelationType relationType, String toEntityName,
											String mappedByPropertyName, IdMapper idMapper, PropertyMapper fakeBidirectionalRelationMapper,
											PropertyMapper fakeBidirectionalRelationIndexMapper, boolean insertable,
											boolean ignoreNotFound) {
		return new RelationDescription(
				fromPropertyName, relationType, toEntityName, mappedByPropertyName, idMapper, fakeBidirectionalRelationMapper,
				fakeBidirectionalRelationIndexMapper, insertable, ignoreNotFound
		);
	}

	public static RelationDescription toMany(String fromPropertyName, RelationType relationType, String toEntityName,
											 String mappedByPropertyName, IdMapper idMapper, PropertyMapper fakeBidirectionalRelationMapper,
											 PropertyMapper fakeBidirectionalRelationIndexMapper, boolean insertable) {
		// Envers populates collections by executing dedicated queries. Special handling of
		// @NotFound(action = NotFoundAction.IGNORE) can be omitted in such case as exceptions
		// (e.g. EntityNotFoundException, ObjectNotFoundException) are never thrown.
		// Therefore assigning false to ignoreNotFound.
		return new RelationDescription(
				fromPropertyName, relationType, toEntityName, mappedByPropertyName, idMapper, fakeBidirectionalRelationMapper,
				fakeBidirectionalRelationIndexMapper, insertable, false
		);
	}

	private RelationDescription(String fromPropertyName, RelationType relationType, String toEntityName,
								String mappedByPropertyName, IdMapper idMapper, PropertyMapper fakeBidirectionalRelationMapper,
								PropertyMapper fakeBidirectionalRelationIndexMapper, boolean insertable, boolean ignoreNotFound) {
		this.fromPropertyName = fromPropertyName;
		this.relationType = relationType;
		this.toEntityName = toEntityName;
		this.mappedByPropertyName = mappedByPropertyName;
		this.ignoreNotFound = ignoreNotFound;
		this.idMapper = idMapper;
		this.fakeBidirectionalRelationMapper = fakeBidirectionalRelationMapper;
		this.fakeBidirectionalRelationIndexMapper = fakeBidirectionalRelationIndexMapper;
		this.insertable = insertable;

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

	public boolean isInsertable() {
		return insertable;
	}

	public boolean isBidirectional() {
		return bidirectional;
	}

	void setBidirectional(boolean bidirectional) {
		this.bidirectional = bidirectional;
	}
}
