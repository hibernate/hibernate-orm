Person person = new Person( 1L );
person.getPhones().add( new Phone( 1L, "landline", "028-234-9876" ) );
person.getPhones().add( new Phone( 2L, "mobile", "072-122-9876" ) );
entityManager.persist( person );