package org.hibernate.envers.synchronization;

import org.hibernate.Session;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;

/**
 * Class responsible for evicting audit data entries that have been stored in the session level cache.
 * This operation increases Envers performance in case of massive entity updates without clearing persistence context.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SessionCacheCleaner {
    /**
     * Schedules audit data removal from session level cache after transaction completion. The operation is performed
     * regardless of commit success.
     * @param session Active Hibernate session.
     * @param data Audit data that shall be evicted (e.g. revision data or entity snapshot)
     */
    public void scheduleAuditDataRemoval(final Session session, final Object data) {
        ((EventSource) session).getActionQueue().registerProcess(new AfterTransactionCompletionProcess() {
            public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
                if (!session.isClosed()) {
                    ((Session) session).evict(data);
                }
            }
        });
    }
}
