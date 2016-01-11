Session session=...;

Club club = session.get( Club.class,1 );

Person p1 = new Person(...);
Person p2 = new Person(...);

club.getMembers().add( p1 );
club.getMembers().add( p2 );

// this evaluates to ... again, it depends
assert club.getMembers.size()==1;