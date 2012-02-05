CriteriaQuery<Person> personCriteria = builder.createQuery( Person.class );
Root<Person> personRoot = person.from( Person.class );
Join<Person,Order> orders = personRoot.join( Person_.orders );
Join<Order,LineItem> orderLines = orders.join( Order_.lineItems );