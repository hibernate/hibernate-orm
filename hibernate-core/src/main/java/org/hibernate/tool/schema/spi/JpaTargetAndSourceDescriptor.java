/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

/**
 * JPA ties the notion of {@link SourceDescriptor} and {@link TargetDescriptor}
 * together: meaning that a SourceDescriptor is specific to a given TargetDescriptor.
 * This contract models that association
 *
 * @author Steve Ebersole
 */
public interface JpaTargetAndSourceDescriptor extends SourceDescriptor, TargetDescriptor {
}
