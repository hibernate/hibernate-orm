Session session = sessionFactory.withOptions()
        .tenantIdentifier( yourTenantIdentifier )
        ...
        .openSession();