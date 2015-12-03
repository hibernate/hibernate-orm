SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();

// Supply an SessionFactory-level Interceptor
sessionFactoryBuilder.applyInterceptor( new MySessionFactoryInterceptor() );

// Add a custom observer
sessionFactoryBuilder.addSessionFactoryObservers( new MySessionFactoryObserver() );

// Apply a CDI BeanManager (for JPA event listeners)
sessionFactoryBuilder.applyBeanManager( getBeanManagerFromSomewhere() );

SessionFactory sessionFactory = sessionFactoryBuilder.build();