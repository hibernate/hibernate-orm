Employee employee = ( Employee )session.createQuery(
    "select e.accessLevel from Employee e where e.userid = :userid and e.password = :password" )
    ...
    .uniqueResult();
