String userid = ...;
String hql = "select e from Employee e join fetch e.projects where e.userid = :userid";
Employee e = (Employee) session.createQuery( hql )
        .setParameter( "userid", userid )
        .uniqueResult();
