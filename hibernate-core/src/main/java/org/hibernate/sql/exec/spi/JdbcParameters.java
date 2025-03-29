/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * The collection
 * @author Steve Ebersole
 */
public interface JdbcParameters {
	void addParameter(JdbcParameter parameter);
	void addParameters(Collection<JdbcParameter> parameters);

	Set<JdbcParameter> getJdbcParameters();

	default void visitJdbcParameters(Consumer<JdbcParameter> jdbcParameterAction) {
		for ( JdbcParameter jdbcParameter : getJdbcParameters() ) {
			jdbcParameterAction.accept( jdbcParameter );
		}
	}
}
