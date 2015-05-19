/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
