MetadataSources sources = ...;
sources.addAnnotatedClass( Address.class );
sources.addAnnotatedClass( Name.class );
sources.addAnnotatedClass( Contact.class );

Metadata metadata = sources.getMetadataBuilder()
		.applyImplicitNamingStrategy( ImplicitNamingStrategyComponentPathImpl.INSTANCE )
		...
		.build();