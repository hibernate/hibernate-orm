Session session = sessionFactory.openSession();
Transaction tx = session.beginTransaction();

String hqlInsert = "insert into DelinquentAccount (id, name) select c.id, c.name from Customer c where ...";
int createdEntities = session.createQuery( hqlInsert )
        .executeUpdate();
tx.commit();
session.close();
