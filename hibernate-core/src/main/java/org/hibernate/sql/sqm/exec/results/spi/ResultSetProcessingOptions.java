/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.spi;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface ResultSetProcessingOptions {
	Object getEffectiveOptionalObject();
	String getEffectiveOptionalEntityName();
	Serializable getEffectiveOptionalId();

	boolean shouldReturnProxies();
}
