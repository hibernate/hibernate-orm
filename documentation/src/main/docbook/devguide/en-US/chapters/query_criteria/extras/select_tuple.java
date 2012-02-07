CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
Root<Person> personRoot = criteria.from( Person.class );
Path<Long> idPath = personRoot.get( Person_.id );
Path<Integer> agePath = personRoot.get( Person_.age );
criteria.multiselect( idPath, agePath );
criteria.where( builder.equal( personRoot.get( Person_.eyeColor ), "brown" ) );

List<Tuple> tuples = em.createQuery( criteria ).getResultList();
for ( Tuple tuple : valueArray ) {
    assert tuple.get( 0 ) == tuple.get( idPath );
	assert tuple.get( 1 ) == tuple.get( agePath );
	...
}