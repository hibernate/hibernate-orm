/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.spi;

import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;

/**
 * @author Steve Ebersole
 */
public interface LoadQueryDetails {
	public String getSqlStatement();

	public ResultSetProcessor getResultSetProcessor();

}
