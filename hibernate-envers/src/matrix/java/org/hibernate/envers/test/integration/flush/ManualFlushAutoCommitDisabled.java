package org.hibernate.envers.test.integration.flush;

import java.util.Properties;

import org.hibernate.FlushMode;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7017")
public class ManualFlushAutoCommitDisabled extends ManualFlush {

	@Override
	public void addConfigurationProperties(Properties configuration) {
		super.addConfigurationProperties( configuration );
		configuration.setProperty("hibernate.connection.autocommit", "false");
	}
}
