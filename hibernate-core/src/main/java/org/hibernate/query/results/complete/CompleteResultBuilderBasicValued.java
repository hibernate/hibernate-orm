/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results.complete;

import org.hibernate.query.results.ResultBuilderBasicValued;

/**
 * Nominal extension to ResultBuilder for cases involving scalar results
 *
 * @author Steve Ebersole
 */
public interface CompleteResultBuilderBasicValued extends CompleteResultBuilder, ResultBuilderBasicValued {
}
