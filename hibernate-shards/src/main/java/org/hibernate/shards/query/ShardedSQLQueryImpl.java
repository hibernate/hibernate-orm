package org.hibernate.shards.query;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.SQLQuery;
import org.hibernate.shards.Shard;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.type.Type;

import java.util.List;

/**
 * @author aviadl@sentrigo.com (Aviad Lichtenstadt)
 */
public class ShardedSQLQueryImpl extends ShardedQueryImpl implements ShardedSQLQuery {

    public ShardedSQLQueryImpl(final QueryId queryId, final List<Shard> shards,
                               final QueryFactory queryFactory, final ShardAccessStrategy shardAccessStrategy) {
        super(queryId, shards, queryFactory, shardAccessStrategy);
    }

    @Override
    public SQLQuery addEntity(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addEntity(Class entityClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addEntity(String alias, String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addEntity(String tableAlias, String entityName, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addEntity(String alias, Class entityClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addEntity(String tableAlias, Class entityName, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addJoin(String alias, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addJoin(String tableAlias, String path, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addScalar(String columnAlias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addScalar(String columnAlias, Type type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RootReturn addRoot(String tableAlias, String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RootReturn addRoot(String tableAlias, Class entityType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addSynchronizedEntityClass(Class entityClass) throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addSynchronizedEntityName(String entityName)
            throws MappingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery addSynchronizedQuerySpace(String querySpace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery setResultSetMapping(String name) {
        throw new UnsupportedOperationException();
    }
}
