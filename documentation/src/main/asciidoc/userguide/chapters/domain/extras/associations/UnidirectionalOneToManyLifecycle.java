Person person = new Person();
Phone phone1 = new Phone("123-456-7890");
Phone phone2 = new Phone("321-654-0987");

person.getPhones().add(phone1);
person.getPhones().add(phone2);
entityManager.persist(person);
entityManager.flush();

person.getPhones().remove(phone1);