package org.hibernate.envers.test.integration.reventity;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OverrideRevListenerTest extends GloballyConfiguredRevListenerTest {
    @Override
    public void configure(Ejb3Configuration cfg) {
        super.configure(cfg);
        cfg.addAnnotatedClass(ListenerRevEntity.class);
    }
}
