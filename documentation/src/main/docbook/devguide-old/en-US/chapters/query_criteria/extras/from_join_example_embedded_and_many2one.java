CriteriaQuery<Person> personCriteria = builder.createQuery( Person.class );
Root<Person> personRoot = person.from( Person.class );
// Person.address is an embedded attribute
Join<Person,Address> personAddress = personRoot.join( Person_.address );
// Address.country is a ManyToOne
Join<Address,Country> addressCountry = personAddress.join( Address_.country );