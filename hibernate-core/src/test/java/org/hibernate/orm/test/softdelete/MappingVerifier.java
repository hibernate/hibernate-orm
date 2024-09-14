/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.metamodel.mapping.SoftDeleteMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class MappingVerifier {
	public static void verifyMapping(
			SoftDeleteMapping mapping,
			String expectedColumnName,
			String expectedTableName,
			Object expectedDeletedLiteralValue) {
		assertThat( mapping ).isNotNull();
		assertThat( mapping.getColumnName() ).isEqualTo( expectedColumnName );
		assertThat( mapping.getTableName() ).isEqualTo( expectedTableName );
		assertThat( mapping.getDeletedLiteralValue() ).isEqualTo( expectedDeletedLiteralValue );
	}
}
