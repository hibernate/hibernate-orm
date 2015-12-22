person.addPhone(new Phone(1L, "landline", "028-234-9876"));
person.addPhone(new Phone(2L, "mobile", "072-122-9876"));
entityManager.flush();
person.removePhone(person.getPhones().get(0));