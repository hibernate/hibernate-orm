MyEntity result = ( MyEntity ) session.createQuery(
		"select e from MyEntity e where e.code = :code" )
    .setParameter( "code", 123 )
    .uniqueResult();
