package org.hibernate.shards.engine;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SharedSessionBuilder;

import java.sql.Connection;

/**
 * Specialized {@link SessionBuilder} with access to stuff from another session
 *
 * @author Steve Ebersole
 * @author Adriano Machado
 */
public interface ShardedSessionBuilder extends SharedSessionBuilder {

    /**
     * Signifies the interceptor from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     */
    ShardedSessionBuilder interceptor();

    /**
     * Signifies that the connection from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     */
    ShardedSessionBuilder connection();

    /**
     * Signifies that the connection release mode from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     */
    ShardedSessionBuilder connectionReleaseMode();

    /**
     * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     */
    ShardedSessionBuilder autoJoinTransactions();

    /**
     * Signifies that the autoClose flag from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     *
     * @deprecated For same reasons as {@link SessionBuilder#autoClose(boolean)} was deprecated.  However, shared
     * session builders can use {@link #autoClose(boolean)} since they do not "inherit" the owner.
     */
    @Deprecated
    ShardedSessionBuilder autoClose();

    /**
     * Signifies that the flushBeforeCompletion flag from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     */
    ShardedSessionBuilder flushBeforeCompletion();

    /**
     * Signifies that the transaction context from the original session should be used to create the new session
     *
     * @return {@code this}, for method chaining
     */
    ShardedSessionBuilder transactionContext();

    @Override
    ShardedSessionBuilder interceptor(Interceptor interceptor);

    @Override
    ShardedSessionBuilder noInterceptor();

    @Override
    ShardedSessionBuilder connection(Connection connection);

    @Override
    ShardedSessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

    @Override
    ShardedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

    @Override
    @Deprecated
    ShardedSessionBuilder autoClose(boolean autoClose);

    @Override
    ShardedSessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion);
}
