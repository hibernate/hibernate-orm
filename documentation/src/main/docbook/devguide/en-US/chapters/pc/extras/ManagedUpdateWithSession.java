Cat cat = session.get( Cat.class, catId );
cat.setName( "Garfield" );
session.flush(); // generally this is not explicitly needed