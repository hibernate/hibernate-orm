/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class ComponentDescription {

	private final ComponentType type;
	private final String propertyName;
	private final String auditMiddleEntityName;
	private final MiddleIdData middleIdData;
	private final ComponentAuditingData auditingData;

	private ComponentDescription(ComponentType type,
			String propertyName,
			String auditMiddleEntityName,
			MiddleIdData middleIdData,
			ComponentAuditingData componentAuditingData) {
		this.type = type;
		this.propertyName = propertyName;
		this.auditMiddleEntityName = auditMiddleEntityName;
		this.middleIdData = middleIdData;
		this.auditingData = componentAuditingData;
	}

	public static ComponentDescription many(final String propertyName, final String auditMiddleEntityName, final MiddleIdData middleIdData) {
		return new ComponentDescription( ComponentType.MANY, propertyName, auditMiddleEntityName, middleIdData, null );
	}

	public static ComponentDescription one(final String propertyName, final ComponentAuditingData componentAuditingData) {
		return new ComponentDescription( ComponentType.ONE, propertyName, null, null, componentAuditingData );
	}

	public ComponentType getType() {
		return type;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getAuditMiddleEntityName() {
		return auditMiddleEntityName;
	}

	public MiddleIdData getMiddleIdData() {
		return middleIdData;
	}

	public ComponentAuditingData getAuditingData() {
		return auditingData;
	}

	public enum ComponentType {
		ONE, MANY
	}
}
