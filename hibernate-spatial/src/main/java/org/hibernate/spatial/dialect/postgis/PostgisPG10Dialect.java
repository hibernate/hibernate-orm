/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.postgis;


import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

@Deprecated
public class PostgisPG10Dialect  extends PostgreSQLDialect {

	public PostgisPG10Dialect(DialectResolutionInfo resolutionInfo) {
		super( resolutionInfo );
	}

	public PostgisPG10Dialect() {
		super( DatabaseVersion.make( 10 ) );
	}

}
