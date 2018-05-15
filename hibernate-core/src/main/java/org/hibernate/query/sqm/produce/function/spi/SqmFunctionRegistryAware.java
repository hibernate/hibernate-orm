/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;

/**
 * Optional interface for {@link SqmFunctionTemplate} implementations that
 * would like to have access to the {@link SqmFunctionRegistry}.
 * <p/>
 * Mainly this will be "emulation" function templates that would access
 * the registry to gain access to any templates for the functions
 * used to perform the emulation.
 *
 * @author Steve Ebersole
 */
public interface SqmFunctionRegistryAware extends SqmFunctionTemplate {
	void injectRegistry(SqmFunctionRegistry registry);
}
