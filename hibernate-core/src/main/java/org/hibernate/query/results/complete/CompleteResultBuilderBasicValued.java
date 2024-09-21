/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
