/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lob;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Test eager materialization and mutation data mapped by
 * #{@link org.hibernate.type.StandardBasicTypes#TEXT}.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/TextMappings.hbm.xml"
)
public class TextTest extends LongStringTest {
}
