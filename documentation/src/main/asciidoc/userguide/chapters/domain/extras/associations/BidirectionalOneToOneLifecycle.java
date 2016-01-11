Phone phone = new Phone("123-456-7890");
PhoneDetails details = new PhoneDetails("T-Mobile", "GSM");

phone.addDetails(details);
entityManager.persist(phone);