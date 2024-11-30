/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.SessionBuilder;

/**
 * Defines the internal contract between the {@link SessionBuilder} and
 * other parts of Hibernate.
 *
 * @see SessionBuilder
 *
 * @author Gail Badner
 */
public interface SessionBuilderImplementor extends SessionBuilder {
}
