/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.from.TableGroupProducer;

/**
 * A generator for new incremental SQL aliases based on a stem
 *
 * @author Steve Ebersole
 */
public interface SqlAliasBase {
	/**
	 * The stem for unique alias generation
	 */
	String getAliasStem();

	/**
	 * Generates a new alias based on the stem
	 */
	String generateNewAlias();

	static SqlAliasBase from(
			SqlAliasBase explicitBase,
			String explicitSourceAlias,
			TableGroupProducer producer,
			SqlAliasBaseGenerator generator) {
		if ( explicitBase != null ) {
			return explicitBase;
		}

		final String baseName;
//		if ( explicitSourceAlias != null && !Character.isDigit( explicitSourceAlias.charAt( 0 ) ) ) {
//			baseName = explicitSourceAlias;
//		}
//		else {
			baseName = producer.getSqlAliasStem();
//		}

		return generator.createSqlAliasBase( baseName );
	}
}
