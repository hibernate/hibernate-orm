/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.exception;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotAuditedException extends AuditException {
	private static final long serialVersionUID = 4809674577449455510L;

	private final String entityName;

	public NotAuditedException(String entityName, String message) {
		super( message );
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}
}
