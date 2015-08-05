Session s = openSession();
Transaction tx = s.beginTransaction();

// Create a customer entity
Map david = new HashMap();
david.put("name", "David");

// Create an organization entity
Map foobar = new HashMap();
foobar.put("name", "Foobar Inc.");

// Link both
david.put("organization", foobar);

// Save both
s.save("Customer", david);
s.save("Organization", foobar);

tx.commit();
s.close();