package org.hibernate.temporal.audit;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * XML-mapped variant of {@link AbstractChangelogEntityTest}: the same
 * entity model and test logic as {@link ChangelogAnnotationTest}, but the
 * mapping is supplied entirely by metadata-complete XML
 * ({@code ChangelogXmlMappingTest.xml}) rather than annotations.
 */
@JiraKey("HHH-20617")
@AuditedTest
@SessionFactory
@DomainModel(
		annotatedClasses = {
				AbstractChangelogEntityTest.Book.class,
				AbstractChangelogEntityTest.MyRevisionInfo.class
		},
		xmlMappings = "org/hibernate/temporal/audit/ChangelogXmlMappingTest.xml"
)
class ChangelogXmlMappingTest extends AbstractChangelogEntityTest {
}
