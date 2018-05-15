/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;

/**
 * Data that is used by all collection mappers, regardless of the type.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class CommonCollectionMapperData {
	private final AuditMetadataBuildingOptions options;
	private final String versionsMiddleEntityName;
	private final PropertyData collectionReferencingPropertyData;
	private final MiddleIdData referencingIdData;
	private final RelationQueryGenerator queryGenerator;
	private final String collectionRole;

	public CommonCollectionMapperData(
			AuditMetadataBuildingOptions options,
			String versionsMiddleEntityName,
			PropertyData collectionReferencingPropertyData,
			MiddleIdData referencingIdData,
			RelationQueryGenerator queryGenerator,
			String collectionRole) {
		this.options = options;
		this.versionsMiddleEntityName = versionsMiddleEntityName;
		this.collectionReferencingPropertyData = collectionReferencingPropertyData;
		this.referencingIdData = referencingIdData;
		this.queryGenerator = queryGenerator;
		this.collectionRole = collectionRole;
	}

	public AuditMetadataBuildingOptions getOptions() {
		return options;
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
