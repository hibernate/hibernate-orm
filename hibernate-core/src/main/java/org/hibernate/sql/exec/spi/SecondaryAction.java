/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

/**
 * Common marker interface for {@linkplain PreAction} and {@linkplain PostAction}.
 *
 * @implSpec Split to allow implementing both simultaneously.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SecondaryAction {
}
