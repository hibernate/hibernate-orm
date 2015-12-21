Phone phone = new Phone("123-456-7890");
PhoneDetails details = new PhoneDetails("T-Mobile", "GSM");

phone.addDetails(details);
entityManager.persist(phone);

PhoneDetails otherDetails = new PhoneDetails("T-Mobile", "CDMA");
otherDetails.setPhone(phone);
entityManager.persist(otherDetails);
entityManager.flush();
entityManager.clear();

//throws javax.persistence.PersistenceException: org.hibernate.HibernateException: More than one row with the given identifier was found: 1
entityManager.find(Phone.class, phone.getId()).getDetails().getProvider();