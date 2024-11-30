/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.boot.spi.SecondPass;

/**
 * Marker interface for second passes which bind id generators
 *
 * @author Steve Ebersole
 */
public interface IdGeneratorResolver extends SecondPass {
}
