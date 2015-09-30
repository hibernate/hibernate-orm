javax.persistence.PersistenceUtil jpaUtil = javax.persistence.Persistence.getPersistenceUtil();
if ( jpaUtil.isLoaded( customer.getAddress() ) {
    //display address if loaded
}
if ( jpaUtil.isLoaded( customer.getOrders()) ) ) {
    //display orders if loaded
}
if (jpaUtil.isLoaded(customer, "detailedBio") ) {
    //display property detailedBio if loaded
}