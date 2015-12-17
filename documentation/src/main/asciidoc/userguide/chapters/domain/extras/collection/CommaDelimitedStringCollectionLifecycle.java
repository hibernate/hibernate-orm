person.phones.add("027-123-4567");
person.phones.add("028-234-9876");
session.flush();

person.getPhones().remove(0);
session.flush();