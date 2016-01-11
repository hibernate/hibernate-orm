int createdEntities = session.createQuery(
    "insert into DelinquentAccount " +
    "   ( id, name ) " +
    "select " +
    "   c.id, c.name " +
    "from Customer c " +
    "where ..."
).executeUpdate();