Person person = new Person();
entityManager.persist(person);

Phone phone = new Phone("123-456-7890");
phone.setPerson(person);
entityManager.persist(phone);

entityManager.flush();
phone.setPerson(null);