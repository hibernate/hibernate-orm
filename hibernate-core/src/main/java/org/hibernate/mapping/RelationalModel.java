/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;

/**
 * A relational object which may be created using DDL
 * @author Gavin King
 *
 * @deprecated (since 5.2) not needed anymore.
 */
@Deprecated
public interface RelationalModel {
	String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) throws HibernateException;
	String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema);
}
