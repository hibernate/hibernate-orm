BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
        // pass in org.hibernate.integrator.spi.Integrator instances which are not
        // auto-discovered (for whatever reason) but which should be included
        .with( anExplicitIntegrator )
        // pass in a class-loader Hibernate should use to load application classes
        .withApplicationClassLoader( anExplicitClassLoaderForApplicationClasses )
        // pass in a class-loader Hibernate should use to load resources
        .withResourceClassLoader( anExplicitClassLoaderForResources )
        // see BootstrapServiceRegistryBuilder for rest of available methods
        ...
        // finally, build the bootstrap registry with all the above options
        .build();
