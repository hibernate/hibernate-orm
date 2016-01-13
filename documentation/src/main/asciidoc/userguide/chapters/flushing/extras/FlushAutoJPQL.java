entityManager.persist( new Person( "Vlad" ) );
entityManager.createQuery( "select p from Phone p" ).getResultList();
entityManager.createQuery( "select p from Person p" ).getResultList();