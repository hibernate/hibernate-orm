/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
