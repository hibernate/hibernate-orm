CriteriaQuery query = builder.createQuery();
Root<Person> men = query.from( Person.class );
Root<Person> women = query.from( Person.class );
Predicate menRestriction = builder.and(
		builder.equal( men.get( Person_.gender ), Gender.MALE ),
		builder.equal( men.get( Person_.relationshipStatus ), RelationshipStatus.SINGLE )
);
Predicate womenRestriction = builder.and(
		builder.equal( women.get( Person_.gender ), Gender.FEMALE ),
		builder.equal( women.get( Person_.relationshipStatus ), RelationshipStatus.SINGLE )
);
query.where( builder.and( menRestriction, womenRestriction ) );