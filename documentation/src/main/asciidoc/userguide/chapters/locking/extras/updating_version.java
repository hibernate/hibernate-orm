Session session = sessionFactory.openSession();
Transaction tx = session.beginTransaction();

int updatedEntities = session.createQuery(
    "update versioned Customer set name = :newName where name = :oldName" )
    .setString( "newName",newName )
    .setString( "oldName",oldName )
    .executeUpdate();

tx.commit();
session.close();
