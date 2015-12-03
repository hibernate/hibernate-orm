String qry = "select e from MyEntity e " +
		" where e.code = :code"
MyEntity result = (MyEntity) session.createQuery( qry )
    .setParameter( "code", 123 )
    .uniqueResult();
