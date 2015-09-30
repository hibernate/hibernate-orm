String userid = ...;
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<Employee> criteria = cb.createQuery( Employee.class );
Root<Employee> root = criteria.from( Employee.class );
root.fetch( Employee_.projects );
criteria.select( root );
criteria.where(
	cb.equal( root.get( Employee_.userid ), cb.literal( userid ) )
);
Employee e = entityManager.createQuery( criteria ).getSingleResult();
