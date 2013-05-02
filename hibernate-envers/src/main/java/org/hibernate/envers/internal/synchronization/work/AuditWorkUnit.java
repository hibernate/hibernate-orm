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
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.RevisionType;

/**
 * TODO: refactor constructors into factory methods
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditWorkUnit extends WorkUnitMergeVisitor, WorkUnitMergeDispatcher {
	Serializable getEntityId();

	String getEntityName();

	boolean containsWork();

	boolean isPerformed();

	/**
	 * Perform this work unit in the given session.
	 *
	 * @param session Session, in which the work unit should be performed.
	 * @param revisionData The current revision data, which will be used to populate the work unit with the correct
	 * revision relation.
	 */
	void perform(Session session, Object revisionData);

	void undo(Session session);

	/**
	 * @param revisionData The current revision data, which will be used to populate the work unit with the correct
	 * revision relation.
	 *
	 * @return Generates data that should be saved when performing this work unit.
	 */
	Map<String, Object> generateData(Object revisionData);

	/**
	 * @return Performed modification type.
	 */
	RevisionType getRevisionType();
}
