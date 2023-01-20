/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Allows custom function descriptors to be contributed to the eventual
 * {@link SqmFunctionRegistry}, either by a {@link org.hibernate.dialect.Dialect}
 * or by a {@link FunctionContributor}.
 *
 * @see FunctionContributor
 *
 * @author Christian Beikov
 */
public interface FunctionContributions {

	/**
	 * The registry into which the contributions should be made.
	 */
	SqmFunctionRegistry getFunctionRegistry();

	/**
	 * Access to type information.
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to {@linkplain Service services}.
	 */
	ServiceRegistry getServiceRegistry();

	/**
	 * The {@linkplain Dialect SQL Dialect}.
	 */
	default Dialect getDialect() {
		return getTypeConfiguration().getCurrentBaseSqlTypeIndicators().getDialect();
	}
}
