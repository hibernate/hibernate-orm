MetadataSources sources = new MetadataSources( standardRegistry );

// alternatively, we can build the MetadataSources without passing
// a service registry, in which case it will build a default
// BootstrapServiceRegistry to use.  But the approach shown
// above is preferred
// MetadataSources sources = new MetadataSources();

// add a class using JPA/Hibernate annotations for mapping
sources.addAnnotatedClass( MyEntity.class );

// add the name of a class using JPA/Hibernate annotations for mapping.
// differs from above in that accessing the Class is deferred which is
// important if using runtime bytecode-enhancement
sources.addAnnotatedClassName( "org.hibernate.example.Customer" );

// Adds the named hbm.xml resource as a source: which performs the
// classpath lookup and parses the XML
sources.addResource( "org/hibernate/example/Order.hbm.xml" );

// Adds the named JPA orm.xml resource as a source: which performs the
// classpath lookup and parses the XML
sources.addResource( "org/hibernate/example/Product.orm.xml" );