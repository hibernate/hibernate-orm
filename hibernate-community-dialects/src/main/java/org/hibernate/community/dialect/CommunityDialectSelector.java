/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		return switch ( name ) {
			case "Cache71", "Cache" -> CacheDialect.class;
			case "CUBRID" -> CUBRIDDialect.class;
			case "Altibase" -> AltibaseDialect.class;
			case "Firebird" -> FirebirdDialect.class;
			case "Informix10", "Informix" -> InformixDialect.class;
			case "Ingres9", "Ingres10", "Ingres" -> IngresDialect.class;
			case "MimerSQL" -> MimerSQLDialect.class;
			case "RDMSOS2200" -> RDMSOS2200Dialect.class;
			case "SAPDB", "MaxDB" -> MaxDBDialect.class;
			case "SybaseAnywhere" -> SybaseAnywhereDialect.class;
			case "Teradata14", "Teradata" -> TeradataDialect.class;
			case "TimesTen" -> TimesTenDialect.class;
			case "SingleStore" -> SingleStoreDialect.class;
			case "Derby" -> DerbyDialect.class;
			default -> null;
		};
	}

}
