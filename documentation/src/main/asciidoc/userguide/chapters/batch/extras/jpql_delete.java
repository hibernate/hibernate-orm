EntityManager entityManager = entityManagerFactory.createEntityManager();
EntityTransaction tx = entityManager.getTransaction();

String jpqlDelete = "delete Customer c where c.name = :oldName";
// or String jpqlDelete = "delete Customer where name = :oldName";
int updatedEntities = entityManager.createQuery( jpqlDelete )
	.setParameter( "oldName", oldName )
	.executeUpdate();

tx.commit();
entityManager.close();