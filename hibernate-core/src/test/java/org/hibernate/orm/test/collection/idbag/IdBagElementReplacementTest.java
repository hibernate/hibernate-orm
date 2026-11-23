package org.hibernate.orm.test.collection.idbag;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which plans the row changes in
 * {@code BasicCollectionDecomposer}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractIdBagElementReplacementTest.Student.class,
				AbstractIdBagElementReplacementTest.Course.class
		}
)
@SessionFactory
public class IdBagElementReplacementTest extends AbstractIdBagElementReplacementTest {
}
