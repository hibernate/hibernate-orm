Session session = sessionFactory.openSession();
Transaction tx = session.beginTransaction();
   
for ( int i=0; i<100000; i++ ) {
    Customer customer = new Customer(.....);
    session.save(customer);
    if ( i % 20 == 0 ) { //20, same as the JDBC batch size
        //flush a batch of inserts and release memory:
        session.flush();
        session.clear();
    }
}
   
tx.commit();
session.close();