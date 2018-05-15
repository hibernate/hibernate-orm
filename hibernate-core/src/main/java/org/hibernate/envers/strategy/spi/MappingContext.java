/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy.spi;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.envers.boot.spi.AuditServiceOptions;

import org.dom4j.Element;

/**
 * Describes an audit mapping context.
 *
 * @author Chris Cranford
 */
@Incubating(since = "5.4")
public class MappingContext {
	private Element auditEntityMapping;
	private Element revisionEntityMapping;
	private AuditServiceOptions options;
	private Dialect dialect;

	public MappingContext(Element auditEntityMapping, Element revisionEntityMapping, AuditServiceOptions options, Dialect dialect) {
		this.auditEntityMapping = auditEntityMapping;
		this.revisionEntityMapping = revisionEntityMapping;
		this.options = options;
		this.dialect = dialect;
	}

	public Element getAuditEntityMapping() {
		return auditEntityMapping;
	}

	public Element getRevisionEntityMapping() {
		return revisionEntityMapping;
	}

	public AuditServiceOptions getOptions() {
		return options;
	}

	public Dialect getDialect() {
		return dialect;
	}
}
