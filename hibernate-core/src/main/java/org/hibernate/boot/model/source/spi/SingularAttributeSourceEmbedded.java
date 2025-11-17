/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Represents the binding source for a singular attribute that is "embedded"
 * or "composite".
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeSourceEmbedded extends SingularAttributeSource, EmbeddableSourceContributor {
}
