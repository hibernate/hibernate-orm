package org.hibernate.orm.test.collection.idbag;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractIdBagAddAndRemoveTest.Student.class,
				AbstractIdBagAddAndRemoveTest.Course.class
		}
)
@SessionFactory
public class IdBagAddAndRemoveTest extends AbstractIdBagAddAndRemoveTest {
}
