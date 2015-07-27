BootstrapServiceRegistryBuilder bootstrapRegistryBuilder
		= new BootstrapServiceRegistryBuilder();

// add a special ClassLoader
bootstrapRegistryBuilder.applyClassLoader( mySpecialClassLoader );
// manually add an Integrator
bootstrapRegistryBuilder.applyIntegrator( mySpecialIntegrator );
...

BootstrapServiceRegistry bootstrapRegistry = bootstrapRegistryBuilder.build();