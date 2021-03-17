/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.norevision;

import java.util.Arrays;
import java.util.List;

public class CollectionChangeRevisionTest extends AbstractCollectionChangeTest {
	@Override
	protected String getCollectionChangeValue() {
		return "true";
	}

	@Override
	protected List<Integer> getExpectedPersonRevisions() {
		return Arrays.asList( 1, 3 );
	}

	@Override
	protected List<Integer> getExpectedParentRevisions() {
		return Arrays.asList( 4, 5 );
	}

	@Override
	protected List<Integer> getExpectedHouseRevisions() {
		return Arrays.asList( 6, 7 );
	}
}
