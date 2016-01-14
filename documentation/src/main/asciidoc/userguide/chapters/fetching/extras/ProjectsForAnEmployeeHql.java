String userid = ...;
Employee e = ( Employee )session.createQuery(
    "select e from Employee e join fetch e.projects where e.userid = :userid" )
    .setParameter( "userid", userid )
    .uniqueResult();
