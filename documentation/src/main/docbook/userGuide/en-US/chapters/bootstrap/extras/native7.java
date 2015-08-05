MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

// Use the JPA-compliant implicit naming strategy
metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );

// specify the schema name to use for tables, etc when none is explicitly specified
metadataBuilder.applyImplicitSchemaName( "my_default_schema" );

Metadata metadata = metadataBuilder.build();