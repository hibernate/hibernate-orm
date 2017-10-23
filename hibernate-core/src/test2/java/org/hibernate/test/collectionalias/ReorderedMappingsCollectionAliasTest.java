/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collectionalias;

/**
 * The bug fixed by HHH-7545 showed showed different results depending on the order
 * in which entity mappings were processed.
 *
 * This mappings are in the opposite order here than in CollectionAliasTest.
 *
 * @Author Gail Badner
 */
public class ReorderedMappingsCollectionAliasTest extends CollectionAliasTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				ATable.class,
				TableA.class,
				TableB.class,
				TableBId.class,
		};
	}
}
