Session session1=...;
Session session2=...;

Club club = session1.get( Club.class,1 );

Person p1 = session1.get( Person.class,1 );
Person p2 = session2.get( Person.class,1 );

club.getMembers().add( p1 );
club.getMembers().add( p2 );

// this evaluates to ... well it depends
assert club.getMembers.size()==1;