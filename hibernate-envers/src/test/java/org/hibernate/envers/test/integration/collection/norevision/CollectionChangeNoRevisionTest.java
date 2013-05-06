package org.hibernate.envers.test.integration.collection.norevision;

import java.util.Arrays;
import java.util.List;

public class CollectionChangeNoRevisionTest extends AbstractCollectionChangeTest {
	protected String getCollectionChangeValue() {
		return "false";
	}

	@Override
	protected List<Integer> getExpectedPersonRevisions() {
		return Arrays.asList( 1 );
	}
}
