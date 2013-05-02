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
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.Query;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * TODO: cleanup implementations and extract common code
 * <p/>
 * Implementations of this interface provide a method to generate queries on a relation table (a table used
 * for mapping relations). The query can select, apart from selecting the content of the relation table, also data of
 * other "related" entities.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface RelationQueryGenerator {
	Query getQuery(AuditReaderImplementor versionsReader, Object primaryKey, Number revision, boolean removed);
}
