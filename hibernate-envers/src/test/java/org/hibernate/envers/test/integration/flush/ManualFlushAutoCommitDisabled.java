package org.hibernate.envers.test.integration.flush;

import org.hibernate.testing.TestForIssue;

import java.util.Map;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7017")
public class ManualFlushAutoCommitDisabled extends ManualFlush {
    @Override
    protected void addConfigOptions(Map options) {
        super.addConfigOptions(options);
        options.put("hibernate.connection.autocommit", "false");
    }
}
