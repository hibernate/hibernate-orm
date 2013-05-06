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
	private final Set<String> auditEntityNames = new HashSet<String>();

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
