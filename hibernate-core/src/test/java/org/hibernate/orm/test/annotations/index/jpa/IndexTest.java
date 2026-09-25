package org.hibernate.orm.test.annotations.index.jpa;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Strong Liu
 */
@DomainModel(
		annotatedClasses = {
				Car.class,
				Dealer.class,
				Importer.class
		}
)
public class IndexTest extends AbstractJPAIndexTest {
}
