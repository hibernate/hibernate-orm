SessionFactory sessionFactory = new Configuration()
		.setInterceptor( new AuditInterceptor() )
		...
		.buildSessionFactory();