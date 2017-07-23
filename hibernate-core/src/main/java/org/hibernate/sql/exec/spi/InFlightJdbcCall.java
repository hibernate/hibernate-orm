/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

/**
 * todo (6.0) - convert this to a Builder paradigm
 *
 * @author Steve Ebersole
 */
public interface InFlightJdbcCall extends JdbcCall {

	void setFunctionReturn(JdbcCallFunctionReturn functionReturn);

	void addParameterRegistration(JdbcCallParameterRegistration registration);
}
