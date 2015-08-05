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
		// Since we are in CMT, a JTA transaction would
		// already have been started.  This call essentially
		// no-ops
		session.getTransaction().begin();

		doTheWork();

		// Since we did not start the transaction (CMT),
		// we also will not end it.  This call essentially
		// no-ops in terms of transaction handling.
		session.getTransaction().commit();
	}
	catch (Exception e) {
		// again, the rollback call here would no-op (aside from
		// marking the underlying CMT transaction for rollback only).
		if ( session.getTransaction().getStatus() == ACTIVE
				|| session.getTransaction().getStatus() == MARKED_ROLLBACK ) {
			session.getTransaction().rollback();
		}
		// handle the underlying error
	}
	finally {
		session.close();
	}
}