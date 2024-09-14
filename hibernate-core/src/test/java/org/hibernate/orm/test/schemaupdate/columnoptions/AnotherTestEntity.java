/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.columnoptions;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.Table;

@Entity
@Table(name = "ANOTHER_TEST_ENTITY")
@PrimaryKeyJoinColumns(
		value = @PrimaryKeyJoinColumn(name = "joined_fk", options = ColumnOptionsTest.PRIMARY_KEY_JOIN_COLUMN_OPTIONS)
)
public class AnotherTestEntity extends TestEntity {

}
