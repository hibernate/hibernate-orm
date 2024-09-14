/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
