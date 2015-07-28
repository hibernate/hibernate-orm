String loginHql = "select e.accessLevel from Employee e where e.userid = :userid and e.password = :password";
Employee employee = (Employee) session.createQuery( loginHql )
        ...
        .uniqueResult();
