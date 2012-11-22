package org.hibernate.shards.query;

import org.hibernate.LockOptions;
import org.hibernate.Query;

/**
 * @author Adriano Machado
 */
public class SetLockOptionsEvent implements QueryEvent {

    private final LockOptions lockOptions;

    public SetLockOptionsEvent(final LockOptions lockOptions) {
        this.lockOptions = lockOptions;
    }

    @Override
    public void onEvent(Query query) {
        query.setLockOptions(lockOptions);
    }
}
