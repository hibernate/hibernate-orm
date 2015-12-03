Query query = session.createQuery(
    "select e from MyEntity e where e.name like :filter"
);
query.setString( "filter", "D%" );

query = session.createQuery(
    "select e from MyEntity e where e.active = :active"
);
query.setBoolean( "active", true );