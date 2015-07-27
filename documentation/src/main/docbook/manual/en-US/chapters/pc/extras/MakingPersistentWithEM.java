// Using the JPA EntityManager
DomesticCat fritz = new DomesticCat();
fritz.setColor( Color.GINGER );
fritz.setSex( 'M' );
fritz.setName( "Fritz" );
entityManager.persist( fritz );