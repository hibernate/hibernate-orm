if ( Hibernate.isInitialized( customer.getAddress() ) {
    //display address if loaded
}
if ( Hibernate.isInitialized( customer.getOrders()) ) ) {
    //display orders if loaded
}
if (Hibernate.isPropertyInitialized( customer, "detailedBio" ) ) {
    //display property detailedBio if loaded
}