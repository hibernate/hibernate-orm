Session session = ...;

Person p1 = session.get( Person.class, 1 );
Person p2 = session.get( Person.class, 1);

// this evaluates to true
assert p1 == p2;