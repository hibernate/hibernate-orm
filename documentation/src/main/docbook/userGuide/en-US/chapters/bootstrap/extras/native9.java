StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
		.configure( "org/hibernate/example/MyCfg.xml" )
		.build();

Metadata metadata = new MetadataSources( standardRegistry )
		.addAnnotatedClass( MyEntity.class )
		.addAnnotatedClassName( "org.hibernate.example.Customer" )
		.addResource( "org/hibernate/example/Order.hbm.xml" )
		.addResource( "org/hibernate/example/Product.orm.xml" )
		.getMetadataBuilder()
		.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE )
		.build();

SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
		.applyBeanManager( getBeanManagerFromSomewhere() )
		.build();