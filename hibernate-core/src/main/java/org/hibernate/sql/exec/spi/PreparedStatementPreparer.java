/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;

/**
 * Defines a contract for preparing a PreparedStatement for execution.  Generally
 * this gives a chance to apply options to the PreparedStatement after creation.
 *
 * @author Steve Ebersole
 */
public interface PreparedStatementPreparer {
	void prepare(PreparedStatement preparedStatement);
}
