/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.naming.spi;

import org.hibernate.internal.util.Loggable;
import org.hibernate.naming.Identifier;

/**
 * Models the qualified name of a database object.  Some things to keep in
 * mind wrt catalog/schema:<ol>
 *     <li>{@link java.sql.DatabaseMetaData#isCatalogAtStart}</li>
 *     <li>{@link java.sql.DatabaseMetaData#getCatalogSeparator()}</li>
 * </ol>
 * <p/>
 * Also, be careful about the usage of {@link #render}.  If the intention is get get the name
 * as used in the database, the {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment} ->
 * {@link org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter#format} should be
 * used instead.
 *
 * @author Steve Ebersole
 */
public interface QualifiedName extends Loggable {
	Identifier getCatalogName();

	Identifier getSchemaName();

	Identifier getObjectName();

	/**
	 * Returns a String-form of the qualified name.
	 * <p/>
	 * Depending on intention, may not be appropriate.  May want
	 * {@link org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter#format}
	 * instead.  See {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment#getQualifiedObjectNameFormatter}
	 *
	 * @return The string form
	 *
	 * @apiNote Use {@link #toLoggableFragment()} instead
	 *
	 * @deprecated (since 6.0) Use/implement {@link #toLoggableFragment()} instead
	 */
	@Deprecated
	String render();

	@Override
	default String toLoggableFragment() {
		final StringBuilder buffer = new StringBuilder();

		final Identifier catalogName = getCatalogName();
		if ( catalogName != null ) {
			buffer.append( catalogName ).append( '.' );
		}


		final Identifier schemaName = getSchemaName();
		if ( schemaName != null ) {
			buffer.append( schemaName ).append( '.' );
		}

		return buffer.append( getObjectName() ).toString();
	}
}
