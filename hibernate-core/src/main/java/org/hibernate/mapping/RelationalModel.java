/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
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
	@Deprecated
	default String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) throws HibernateException {
		return sqlCreateString( p, SqlStringGenerationContextImpl.forBackwardsCompatibility( dialect, defaultCatalog, defaultSchema ),
				defaultCatalog, defaultSchema );
	}

	String sqlCreateString(Mapping p, SqlStringGenerationContext context, String defaultCatalog, String defaultSchema) throws HibernateException;

	@Deprecated
	default String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) throws HibernateException {
		return sqlDropString( SqlStringGenerationContextImpl.forBackwardsCompatibility( dialect, defaultCatalog, defaultSchema ),
				defaultCatalog, defaultSchema );
	}

	String sqlDropString(SqlStringGenerationContext context, String defaultCatalog, String defaultSchema);
}
