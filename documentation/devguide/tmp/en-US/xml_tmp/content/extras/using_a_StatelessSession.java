StatelessSession session = sessionFactory.openStatelessSession();
Transaction tx = session.beginTransaction();
   
ScrollableResults customers = session.getNamedQuery("GetCustomers")
    .scroll(ScrollMode.FORWARD_ONLY);
while ( customers.next() ) {
    Customer customer = (Customer) customers.get(0);
    customer.updateStuff(...);
    session.update(customer);
}
   
tx.commit();
session.close();