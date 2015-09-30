List results =
    session.createQuery( "select e from MyEntity e" )
        .list();
