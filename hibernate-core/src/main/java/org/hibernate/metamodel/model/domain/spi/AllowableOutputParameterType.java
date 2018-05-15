/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * @apiNote Generally speaking, an allowable output parameter type
 * can only effectively be a basic type.  So generally speaking, the
 * extractor returned here is a simple wrapper around a
 * {@link org.hibernate.sql.JdbcValueExtractor}.  It is conceivable that a
 * composite type could provide an extractor - I'm just not sure that would work
 * with the composite containing collections or to-one relationships. Or
 * conceivably a composite from a "complex" database UDT, etc
 *
 * @author Steve Ebersole
 */
public interface AllowableOutputParameterType<J> extends AllowableParameterType<J>, DomainResultProducer {
}
