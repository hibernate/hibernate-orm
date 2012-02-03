String jpqlUpdate =
		"update Customer c " +
		"set c.name = :newName " +
		"where c.name = :oldName";
int updatedEntities = entityManager.createQuery( jpqlUpdate )
        .setString( "newName", newName )
        .setString( "oldName", oldName )
        .executeUpdate();
