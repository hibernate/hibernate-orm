/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
