Person person1 = new Person();
Person person2 = new Person();

Address address1 = new Address("12th Avenue", "12A");
Address address2 = new Address("18th Avenue", "18B");

person1.getAddresses().add(address1);
person1.getAddresses().add(address2);

person2.getAddresses().add(address1);

entityManager.persist(person1);
entityManager.persist(person2);

entityManager.flush();

person1.getAddresses().remove(address1);
