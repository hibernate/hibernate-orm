CriteriaQuery<Person> personCriteria = builder.createQuery( Person.class );
Root<Person> personRoot = person.from( Person.class );
// Person.address is an embedded attribute
Fetch<Person,Address> personAddress = personRoot.fetch( Person_.address );
// Address.country is a ManyToOne
Fetch<Address,Country> addressCountry = personAddress.fetch( Address_.country );