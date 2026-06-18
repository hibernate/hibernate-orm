/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.expression.SqmTextExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmTextPath extends SqmPath<String>, SqmTextExpression {
}
