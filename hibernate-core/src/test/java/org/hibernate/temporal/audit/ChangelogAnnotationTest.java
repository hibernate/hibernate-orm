package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Annotation-mapped variant of {@link AbstractChangelogEntityTest}.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AbstractChangelogEntityTest.Book.class,
		AbstractChangelogEntityTest.MyRevisionInfo.class
})
class ChangelogAnnotationTest extends AbstractChangelogEntityTest {
}
