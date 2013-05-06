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
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;

/**
 * Data that is used by all collection mappers, regardless of the type.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public final class CommonCollectionMapperData {
	private final AuditEntitiesConfiguration verEntCfg;
	private final String versionsMiddleEntityName;
	private final PropertyData collectionReferencingPropertyData;
	private final MiddleIdData referencingIdData;
	private final RelationQueryGenerator queryGenerator;

	public CommonCollectionMapperData(
			AuditEntitiesConfiguration verEntCfg, String versionsMiddleEntityName,
			PropertyData collectionReferencingPropertyData, MiddleIdData referencingIdData,
			RelationQueryGenerator queryGenerator) {
		this.verEntCfg = verEntCfg;
		this.versionsMiddleEntityName = versionsMiddleEntityName;
		this.collectionReferencingPropertyData = collectionReferencingPropertyData;
		this.referencingIdData = referencingIdData;
		this.queryGenerator = queryGenerator;
	}

	public AuditEntitiesConfiguration getVerEntCfg() {
		return verEntCfg;
	}

	public String getVersionsMiddleEntityName() {
		return versionsMiddleEntityName;
	}

	public PropertyData getCollectionReferencingPropertyData() {
		return collectionReferencingPropertyData;
	}

	public MiddleIdData getReferencingIdData() {
		return referencingIdData;
	}

	public RelationQueryGenerator getQueryGenerator() {
		return queryGenerator;
	}
}
