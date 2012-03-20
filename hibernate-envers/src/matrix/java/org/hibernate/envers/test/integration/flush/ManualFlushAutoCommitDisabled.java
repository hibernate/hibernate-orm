package org.hibernate.envers.test.integration.flush;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7017")
public class ManualFlushAutoCommitDisabled extends ManualFlush {
    @Override
    public void configure(Ejb3Configuration cfg) {
        super.configure(cfg);
        cfg.setProperty("hibernate.connection.autocommit", "false");
    }
}
