/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * Describes a ResultSet mapping applied to either a {@link org.hibernate.query.NativeQuery}
 * or a {@link org.hibernate.procedure.ProcedureCall} / {@link javax.persistence.StoredProcedureQuery}.
 *
 * It is either generated from a {@link NamedResultSetMappingMemento} or
 * on-the-fly via Hibernate's {@link org.hibernate.query.NativeQuery} contract.  Acts
 * as the {@link DomainResultProducer} for these uses
 *
 * @see org.hibernate.query.NativeQuery#addScalar
 * @see org.hibernate.query.NativeQuery#addEntity
 * @see org.hibernate.query.NativeQuery#addJoin
 * @see org.hibernate.query.NativeQuery#addFetch
 * @see org.hibernate.query.NativeQuery#addRoot
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultSetMapping extends DomainResultProducer {
}
