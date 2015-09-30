Query query = em.createNamedQuery( "my-predefined-named-query" );
TypedQuery<MyEntity> query2 = em.createNamedQuery(
    "my-predefined-named-query",
    MyEntity.class
);