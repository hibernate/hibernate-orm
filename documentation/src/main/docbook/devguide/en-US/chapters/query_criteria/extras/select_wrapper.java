public class PersonWrapper {
	private final Long id;
	private final Integer age;
	public PersonWrapper(Long id, Integer age) {
		this.id = id;
		this.age = age;
	}
	...
}

...

CriteriaQuery<PersonWrapper> criteria = builder.createQuery( PersonWrapper.class );
Root<Person> personRoot = criteria.from( Person.class );
criteria.select(
		builder.construct(
			PersonWrapper.class,
			personRoot.get( Person_.id ),
			personRoot.get( Person_.age )
		)
);
criteria.where( builder.equal( personRoot.get( Person_.eyeColor ), "brown" ) );

List<PersonWrapper> people = em.createQuery( criteria ).getResultList();
for ( PersonWrapper person : people ) {
    ...
}