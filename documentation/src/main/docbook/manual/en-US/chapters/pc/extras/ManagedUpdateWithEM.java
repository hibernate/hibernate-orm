Cat cat = entityManager.find( Cat.class, catId );
cat.setName( "Garfield" );
entityManager.flush(); // generally this is not explicitly needed