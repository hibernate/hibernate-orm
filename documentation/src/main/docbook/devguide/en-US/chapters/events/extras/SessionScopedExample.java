Session session = sf.withOptions().interceptor( new AuditInterceptor() ).openSession();
