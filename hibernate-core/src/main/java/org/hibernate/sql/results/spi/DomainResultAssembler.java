/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Responsible for "assembling" a result for inclusion in the domain query
 * result.  "Assembling" the result basically means building the result object
 * (whatever that means for a specific result type) and returning it for
 * injection into the result "row" currently being processed
 *
 * @author Steve Ebersole
 */
public interface DomainResultAssembler {
}
