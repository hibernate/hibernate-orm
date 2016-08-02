StandardServiceRegistryBuilder builder = ...;
...
builder.addService(
    ConnectionProvider.class,
    new LatestAndGreatestConnectionProviderImpl()
);
...