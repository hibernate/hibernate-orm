package org.hibernate.envers.test.integration.reventity;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6696")
public class OverrideCustomRevListenerTest extends GloballyConfiguredRevListenerTest {
    @Override
    public void configure(Ejb3Configuration cfg) {
        super.configure(cfg);
        cfg.addAnnotatedClass(ListenerRevEntity.class);
    }
}
