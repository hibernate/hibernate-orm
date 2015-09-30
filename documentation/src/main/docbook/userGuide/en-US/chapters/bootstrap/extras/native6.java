MetadataSources sources = new MetadataSources( standardRegistry )
		.addAnnotatedClass( MyEntity.class )
		.addAnnotatedClassName( "org.hibernate.example.Customer" )
		.addResource( "org/hibernate/example/Order.hbm.xml" )
		.addResource( "org/hibernate/example/Product.orm.xml" );