/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public interface Literal extends JdbcParameterBinder, Expression {
	Object getLiteralValue();
	JdbcMapping getJdbcMapping();
}
