String hqlVersionedUpdate =
		"update versioned Customer c " +
		"set c.name = :newName " +
		"where c.name = :oldName";
int updatedEntities = s.createQuery( hqlUpdate )
        .setString( "newName", newName )
        .setString( "oldName", oldName )
        .executeUpdate();