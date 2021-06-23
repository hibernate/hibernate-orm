/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.util.Objects;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;

public class CommunityDialectSelector implements DialectSelector {

	@Override
	public Class<? extends Dialect> resolve(String name) {
		Objects.requireNonNull( name );
		if ( name.isEmpty() ) {
			return null;
		}
		switch ( name ) {
			case "Cache":
				return CacheDialect.class;
			case "Cache71":
				return Cache71Dialect.class;
			case "CUBRID":
				return CUBRIDDialect.class;
			case "Firebird":
				return FirebirdDialect.class;
			case "Informix":
				return InformixDialect.class;
			case "Informix10":
				return Informix10Dialect.class;
			case "Ingres":
				return IngresDialect.class;
			case "Ingres9":
				return Ingres9Dialect.class;
			case "Ingres10":
				return Ingres10Dialect.class;
			case "MimerSQL":
				return MimerSQLDialect.class;
			case "RDMSOS2200":
				return RDMSOS2200Dialect.class;
			case "SAPDB":
				return SAPDBDialect.class;
			case "MaxDB":
				return MaxDBDialect.class;
			case "SybaseAnywhere":
				return SybaseAnywhereDialect.class;
			case "Teradata":
				return TeradataDialect.class;
			case "Teradata14":
				return Teradata14Dialect.class;
			case "TimesTen":
				return TimesTenDialect.class;
		}
		return null;
	}

}
