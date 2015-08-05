public void doSomeWork() {
	StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
			.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" )
			...;

	// Note: depending on the JtaPlatform used and some optional settings,
	// the underlying transactions here will be controlled through either
	// the JTA TransactionManager or UserTransaction

	SessionFactory = ...;

	Session session = sessionFactory.openSession();
	try {
		// Assuming a JTA transaction is not already active,
		// this call the TM/UT begin method.  If a JTA
		// transaction is already active, we remember that
		// the Transaction associated with the Session did
		// not "initiate" the JTA transaction and will later
		// nop-op the commit and rollback calls...
		session.getTransaction().begin();

		doTheWork();

		// calls TM/UT commit method, assuming we are initiator.
		session.getTransaction().commit();
	}
	catch (Exception e) {
		// we may need to rollback depending on
		// where the exception happened
		if ( session.getTransaction().getStatus() == ACTIVE
				|| session.getTransaction().getStatus() == MARKED_ROLLBACK ) {
			// calls TM/UT commit method, assuming we are initiator;
			// otherwise marks the JTA trsnaction for rollback only
			session.getTransaction().rollback();
		}
		// handle the underlying error
	}
	finally {
		session.close();
	}
}