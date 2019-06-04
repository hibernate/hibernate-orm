/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

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
