StandardServiceRegistryBuilder standardRegistryBuilder = ...;

// load some properties via resource lookup
standardRegistryBuilder.loadProperties( "org/hibernate/example/MyProperties.properties" );

// configure the registry from a resource lookup for a cfg.xml config file
standardRegistryBuilder.configure( "org/hibernate/example/my.cfg.xml" );

// apply a random setting
standardRegistryBuilder.applySetting( "myProp", "some value" );

// apply a service initiator
standardRegistryBuilder.addInitiator( new CustomServiceInitiator() );

// apply a service impl
standardRegistryBuilder.addService( SomeCustomService.class, new SomeCustomServiceImpl() );

// and finally build the StandardServiceRegistry
StandardServiceRegistry standardRegistry = standardRegistryBuilder.build();