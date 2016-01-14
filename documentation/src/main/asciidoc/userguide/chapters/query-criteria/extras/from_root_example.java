CriteriaQuery<Person> personCriteria = builder.createQuery( Person.class );
// create and add the root
person.from( Person.class );