/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = { Table2.class, Table1.class, ConfEntity.class, UserConfEntity.class, UserEntity.class }
)
public class MappingReorderedAliasTest extends AliasTest {
}
