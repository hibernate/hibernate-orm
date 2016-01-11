Query query = session.createQuery(
    "select e from MyEntity e where e.name like :filter"
);
query.setParameter( "filter", "D%", StringType.INSTANCE );