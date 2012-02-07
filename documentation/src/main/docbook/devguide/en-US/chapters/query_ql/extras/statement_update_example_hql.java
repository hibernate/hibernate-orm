String hqlUpdate =
		"update Customer c " +
		"set c.name = :newName " +
		"where c.name = :oldName";
int updatedEntities = session.createQuery( hqlUpdate )
        .setString( "newName", newName )
        .setString( "oldName", oldName )
        .executeUpdate();
