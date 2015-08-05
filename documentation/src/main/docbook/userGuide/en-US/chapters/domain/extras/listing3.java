Session session = ...;

Club club = session.get( Club.class, 1 );

Person p1 = session.get( Person.class, 1 );
Person p2 = session.get( Person.class, 1);

club.getMembers().add( p1 );
club.getMembers().add( p2 );

// this evaluates to true
assert club.getMembers.size() == 1;