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
		switch ( name ) {
			case "Cache71":
			case "Cache":
				return CacheDialect.class;
			case "CUBRID":
				return CUBRIDDialect.class;
			case "Altibase":
				return AltibaseDialect.class;
			case "Firebird":
				return FirebirdDialect.class;
			case "Informix10":
			case "Informix":
				return InformixDialect.class;
			case "Ingres9":
			case "Ingres10":
			case "Ingres":
				return IngresDialect.class;
			case "MimerSQL":
				return MimerSQLDialect.class;
			case "RDMSOS2200":
				return RDMSOS2200Dialect.class;
			case "SAPDB":
			case "MaxDB":
				return MaxDBDialect.class;
			case "SybaseAnywhere":
				return SybaseAnywhereDialect.class;
			case "TimesTen":
				return TimesTenDialect.class;
			case "SingleStore":
				return SingleStoreDialect.class;
			case "Derby":
				return DerbyDialect.class;
		}
		return null;
	}

}
