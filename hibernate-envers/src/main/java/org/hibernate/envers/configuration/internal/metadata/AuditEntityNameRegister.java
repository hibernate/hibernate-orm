/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.MappingException;

/**
 * A register of all audit entity names used so far.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditEntityNameRegister {
	private final Set<String> auditEntityNames = new HashSet<>();

	/**
	 * @param auditEntityName Name of the audit entity.
	 *
	 * @return True if the given audit entity name is already used.
	 */
	private boolean check(String auditEntityName) {
		return auditEntityNames.contains( auditEntityName );
	}

	/**
	 * Register an audit entity name. If the name is already registered, an exception is thrown.
	 *
	 * @param auditEntityName Name of the audit entity.
	 */
	public void register(String auditEntityName) {
		if ( auditEntityNames.contains( auditEntityName ) ) {
			throw new MappingException( "The audit entity name '" + auditEntityName + "' is already registered." );
		}

		auditEntityNames.add( auditEntityName );
	}

	/**
	 * Creates a unique (not yet registered) audit entity name by appending consecutive numbers to the base
	 * name. If the base name is not yet used, it is returned unmodified.
	 *
	 * @param baseAuditEntityName The base entity name.
	 *
	 * @return A unique audit entity name
	 */
	public String createUnique(final String baseAuditEntityName) {
		String auditEntityName = baseAuditEntityName;
		int count = 1;
		while ( check( auditEntityName ) ) {
			auditEntityName = baseAuditEntityName + count++;
		}

		return auditEntityName;
	}
}
