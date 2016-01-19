EntityManager entityManager = entityManagerFactory.createEntityManager();
EntityTransaction tx = entityManager.getTransaction();

String jpqlUpdate = "update Customer c set c.name = :newName where c.name = :oldName";
// or String query = "update Customer set name = :newName where name = :oldName";
int updatedEntities = entityManager.createQuery( jpqlUpdate )
	.setParameter( "oldName", oldName )
	.setParameter( "newName", newName )
	.executeUpdate();

tx.commit();
entityManager.close();