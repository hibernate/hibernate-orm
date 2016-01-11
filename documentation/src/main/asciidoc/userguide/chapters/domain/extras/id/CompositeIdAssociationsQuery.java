PersonAddress personAddress = entityManager.find(
    PersonAddress.class,
    new PersonAddress( person, address )
);