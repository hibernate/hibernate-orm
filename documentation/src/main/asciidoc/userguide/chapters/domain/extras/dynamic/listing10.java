Session s = openSession();
Transaction tx = s.beginTransaction();

// Create a customer entity
Map<String, String>david = new HashMap<>();
david.put( "name","David" );

// Create an organization entity
Map<String, String>foobar = new HashMap<>();
foobar.put( "name","Foobar Inc." );

// Link both
david.put( "organization",foobar );

// Save both
s.save( "Customer",david );
s.save( "Organization",foobar );

tx.commit();
s.close();