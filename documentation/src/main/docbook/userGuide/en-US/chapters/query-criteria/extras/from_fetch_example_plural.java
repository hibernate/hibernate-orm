CriteriaQuery&lt;Person&gt; personCriteria = builder.createQuery( Person.class );
Root<Person> personRoot = person.from( Person.class );
Fetch<Person,Order> orders = personRoot.fetch( Person_.orders );
Fetch<Order,LineItem> orderLines = orders.fetch( Order_.lineItems );