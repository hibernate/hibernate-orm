Session session1=...;
Session session2=...;

Person p1 = session1.get( Person.class,1 );
Person p2 = session2.get( Person.class,1 );

// this evaluates to false
assert p1==p2;