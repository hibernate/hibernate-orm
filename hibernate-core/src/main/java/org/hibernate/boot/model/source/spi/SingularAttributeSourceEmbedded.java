/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Represents the binding source for a singular attribute that is "embedded"
 * or "composite".
 *
 * @author Steve Ebersole
 */
@Remove
public interface SingularAttributeSourceEmbedded extends SingularAttributeSource, EmbeddableSourceContributor {
}
