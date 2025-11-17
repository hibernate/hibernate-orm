/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes an {@code <any/>} mapping
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeSourceAny extends SingularAttributeSource, AnyMappingSource, CascadeStyleSource {
}
