/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.PreparableMutationOperation;

/**
 * JdbcOperation extension for model mutations stemming from
 * persistence context flushes
 *
 * @author Steve Ebersole
 */
public interface JdbcMutationOperation extends JdbcOperation, PreparableMutationOperation {
}
