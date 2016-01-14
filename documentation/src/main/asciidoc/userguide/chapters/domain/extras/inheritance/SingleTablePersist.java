DebitAccount debitAccount = new DebitAccount();
debitAccount.setId(1L);
debitAccount.setOwner("John Doe");
debitAccount.setBalance(BigDecimal.valueOf(100));
debitAccount.setInterestRate(BigDecimal.valueOf(1.5d));
debitAccount.setOverdraftFee(BigDecimal.valueOf(25));

CreditAccount creditAccount = new CreditAccount();
creditAccount.setId(2L);
creditAccount.setOwner("John Doe");
creditAccount.setBalance(BigDecimal.valueOf(1000));
creditAccount.setInterestRate(BigDecimal.valueOf(1.9d));
creditAccount.setCreditLimit(BigDecimal.valueOf(5000));

entityManager.persist(debitAccount);
entityManager.persist(creditAccount);