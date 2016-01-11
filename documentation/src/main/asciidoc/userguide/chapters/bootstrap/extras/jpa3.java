String persistenceUnitName = ...
List<String> entityClassNames = ...
Properties properties = ...

PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl(
    persistenceUnitName,
    entityClassNames,
    properties
);

Map<String, Object> integrationSettings = new HashMap<>();
integrationSettings.put(AvailableSettings.INTERCEPTOR, interceptor());

EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
    new PersistenceUnitInfoDescriptor(persistenceUnitInfo),
    integrationSettings
);
EntityManagerFactory emf = entityManagerFactoryBuilder.build();