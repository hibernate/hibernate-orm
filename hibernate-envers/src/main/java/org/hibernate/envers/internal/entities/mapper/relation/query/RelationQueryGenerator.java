/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
