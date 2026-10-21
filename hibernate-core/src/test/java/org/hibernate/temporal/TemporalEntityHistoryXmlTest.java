package org.hibernate.temporal;

import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;

/**
 * XML-mapped variant of {@link AbstractTemporalEntityHistoryTest}: the same
 * entity model and test logic as {@link TemporalEntityHistoryTest}, but the
 * mapping is supplied entirely by metadata-complete XML
 * ({@code TemporalEntityHistoryXmlTest.xml}) rather than annotations.
 */
@JiraKey("HHH-20617")
@SessionFactory
@DomainModel(
		annotatedClasses =
				{AbstractTemporalEntityHistoryTest.TemporalEntity4.class,
				AbstractTemporalEntityHistoryTest.TemporalChild4.class},
		xmlMappings = "org/hibernate/temporal/TemporalEntityHistoryXmlTest.xml"
)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 11, versionMatchMode = VersionMatchMode.OLDER, reason = "See https://jira.mariadb.org/browse/MDEV-39230")
class TemporalEntityHistoryXmlTest extends AbstractTemporalEntityHistoryTest {
}
