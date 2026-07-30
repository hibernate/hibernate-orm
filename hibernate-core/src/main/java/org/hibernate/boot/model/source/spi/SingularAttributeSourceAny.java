/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Describes an {@code <any/>} mapping
 *
 * @author Steve Ebersole
 */
@Remove
public interface SingularAttributeSourceAny extends SingularAttributeSource, AnyMappingSource, CascadeStyleSource {
}
