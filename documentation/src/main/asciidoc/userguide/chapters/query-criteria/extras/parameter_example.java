CriteriaQuery<Person> criteria = build.createQuery( Person.class );
Root<Person> personRoot = criteria.from( Person.class );
criteria.select( personRoot );
ParameterExpression<String> eyeColorParam = builder.parameter( String.class );
criteria.where( builder.equal( personRoot.get( Person_.eyeColor ), eyeColorParam ) );

TypedQuery<Person> query = em.createQuery( criteria );
query.setParameter( eyeColorParam, "brown" );
List<Person> people = query.getResultList();