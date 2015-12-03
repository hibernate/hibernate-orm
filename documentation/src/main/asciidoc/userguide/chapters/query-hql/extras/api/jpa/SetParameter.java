Query query = em.createQuery(
    "select e from MyEntity e where e.name like :filter"
);
query.setParameter( "filter", "D%" );

Query q2 = em.createQuery(
    "select e from MyEntity e where e.activeDate > :activeDate"
);
q2.setParameter( "activeDate", new Date(), TemporalType.DATE );