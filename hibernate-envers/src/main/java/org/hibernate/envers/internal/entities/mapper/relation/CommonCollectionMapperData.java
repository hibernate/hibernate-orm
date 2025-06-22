/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;

/**
 * Data that is used by all collection mappers, regardless of the type.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class CommonCollectionMapperData {
	private final String versionsMiddleEntityName;
	private final PropertyData collectionReferencingPropertyData;
	private final MiddleIdData referencingIdData;
	private final RelationQueryGenerator queryGenerator;
	private final String collectionRole;

	public CommonCollectionMapperData(
			String versionsMiddleEntityName,
			PropertyData collectionReferencingPropertyData,
			MiddleIdData referencingIdData,
			RelationQueryGenerator queryGenerator,
			String collectionRole) {
		this.versionsMiddleEntityName = versionsMiddleEntityName;
		this.collectionReferencingPropertyData = collectionReferencingPropertyData;
		this.referencingIdData = referencingIdData;
		this.queryGenerator = queryGenerator;
		this.collectionRole = collectionRole;
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

	public String getRole() {
		return collectionRole;
	}
}
