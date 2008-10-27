package org.jboss.envers.test.integration.reventity;

import org.jboss.envers.RevisionListener;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestRevisionListener implements RevisionListener {
    public static String data = "data0";

    public void newRevision(Object revisionEntity) {
        ((ListenerRevEntity) revisionEntity).setData(data);
    }
}
