package org.hibernate.envers.test.integration.reventity;

import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.testing.TestForIssue;

import java.util.Properties;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6696")
public class OverrideDefaultRevListenerTest extends GloballyConfiguredRevListenerTest {
    @Override
    protected void revisionEntityForDialect(Ejb3Configuration cfg, Dialect dialect, Properties configurationProperties) {
        cfg.addAnnotatedClass(LongRevNumberRevEntity.class);
    }
}