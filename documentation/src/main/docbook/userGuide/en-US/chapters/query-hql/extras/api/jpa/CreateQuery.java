Query query = em.createQuery(
    "select e from MyEntity e where name like :filter"
);
TypedQuery<MyEntity> query2 = em.createQuery(
    "select e from MyEntity e where name like :filter"
    MyEntity.class
);