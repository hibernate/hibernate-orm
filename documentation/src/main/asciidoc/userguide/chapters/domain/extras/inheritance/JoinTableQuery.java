List<Account> accounts = entityManager
    .createQuery( "select a from Account a" )
    .getResultList();