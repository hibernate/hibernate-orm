package org.hibernate.temporal;

import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.MariaDBDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;

/**
 * Tests {@link TemporalTableStrategy#SINGLE_TABLE}, where the collection table holds every version
 * of its rows.
 */
@SessionFactory
@DomainModel(annotatedClasses = AbstractTemporalIdBagTest.Owner.class)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 11, versionMatchMode = VersionMatchMode.OLDER, reason = "See https://jira.mariadb.org/browse/MDEV-39230")
class TemporalIdBagTest extends AbstractTemporalIdBagTest {
	@Override
	String versionsTable(String collectionTable) {
		return collectionTable;
	}
}
