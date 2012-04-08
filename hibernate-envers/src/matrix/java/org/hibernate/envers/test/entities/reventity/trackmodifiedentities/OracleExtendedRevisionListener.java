package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.RevisionListener;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OracleExtendedRevisionListener implements RevisionListener {
    public static final String COMMENT_VALUE = "User Comment";

    public void newRevision(Object revisionEntity) {
        ((OracleExtendedRevisionEntity)revisionEntity).setUserComment(COMMENT_VALUE);
    }
}
