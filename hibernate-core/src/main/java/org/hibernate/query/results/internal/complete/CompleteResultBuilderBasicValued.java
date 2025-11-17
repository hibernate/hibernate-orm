/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.query.results.ResultBuilderBasicValued;

/**
 * Nominal extension to ResultBuilder for cases involving scalar results
 *
 * @author Steve Ebersole
 */
public interface CompleteResultBuilderBasicValued extends CompleteResultBuilder, ResultBuilderBasicValued {
}
