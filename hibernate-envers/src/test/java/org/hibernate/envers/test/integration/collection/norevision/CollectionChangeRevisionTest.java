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
}
