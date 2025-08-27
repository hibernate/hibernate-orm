/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import org.hibernate.spi.DotIdentifierSequence;
import org.hibernate.query.hql.spi.SemanticPathPart;

/**
 * @author Steve Ebersole
 */
public interface FullyQualifiedReflectivePathSource extends DotIdentifierSequence, SemanticPathPart {
}
