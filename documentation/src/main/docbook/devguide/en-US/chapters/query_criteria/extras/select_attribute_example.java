CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
Root<Person> personRoot = criteria.from( Person.class );
criteria.select( personRoot.get( Person_.age ) );
criteria.where( builder.equal( personRoot.get( Person_.eyeColor ), "brown" ) );

List<Integer> ages = em.createQuery( criteria ).getResultList();
for ( Integer age : ages ) {
	...
}