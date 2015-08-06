CriteriaQuery<Object[]> criteria = builder.createQuery( Object[].class );
Root<Person> personRoot = criteria.from( Person.class );
Path<Long> idPath = personRoot.get( Person_.id );
Path<Integer> agePath = personRoot.get( Person_.age );
criteria.select( builder.array( idPath, agePath ) );
criteria.where( builder.equal( personRoot.get( Person_.eyeColor ), "brown" ) );

List<Object[]> valueArray = em.createQuery( criteria ).getResultList();
for ( Object[] values : valueArray ) {
	final Long id = (Long) values[0];
	final Integer age = (Integer) values[1];
	...
}