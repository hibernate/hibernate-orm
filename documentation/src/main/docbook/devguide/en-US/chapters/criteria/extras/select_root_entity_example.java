CriteriaQuery<Person> criteria = builder.createQuery( Person.class );
Root<Person> personRoot = criteria.from( Person.class );
criteria.select( personRoot );
criteria.where( builder.equal( personRoot.get( Person_.eyeColor ), "brown" ) );

List<Person> people = em.createQuery( criteria ).getResultList();
for ( Person person : people ) {
    ...
}