/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy.spi;

import org.hibernate.Incubating;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;

import org.dom4j.Element;

/**
 * Describes an audit mapping context.
 *
 * @author Chris Cranford
 */
@Incubating
public class MappingContext {
	private Element auditEntityMapping;
	private Element revisionEntityMapping;
	private AuditEntitiesConfiguration auditEntityConfiguration;

	public MappingContext(
			Element auditEntityMapping,
			Element revisionEntityMapping,
			AuditEntitiesConfiguration auditEntitiesConfiguration) {
		this.auditEntityMapping = auditEntityMapping;
		this.revisionEntityMapping = revisionEntityMapping;
		this.auditEntityConfiguration = auditEntitiesConfiguration;
	}

	public Element getAuditEntityMapping() {
		return auditEntityMapping;
	}

	public Element getRevisionEntityMapping() {
		return revisionEntityMapping;
	}

	public AuditEntitiesConfiguration getAuditEntityConfiguration() {
		return auditEntityConfiguration;
	}
}
