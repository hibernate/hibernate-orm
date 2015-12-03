public void doSomeWork() {
	StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
			// "jdbc" is the default, but for explicitness
			.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jdbc" )
			...;

	SessionFactory = ...;

	Session session = sessionFactory.openSession();
	try {
		// calls Connection#setAutoCommit(false) to
		// signal start of transaction
		session.getTransaction().begin();

		doTheWork();

		// calls Connection#commit(), if an error
		// happens we attempt a rollback
		session.getTransaction().commit();
	}
	catch (Exception e) {
		// we may need to rollback depending on
		// where the exception happened
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