Session session = ...;
session.getTransaction().begin();

Club club = session.get( Club.class, 1 );

Person p1 = new Person(...);
Person p2 = new Person(...);

session.save( p1 );
session.save( p2 );
session.flush();

club.getMembers().add( p1 );
club.getMembers().add( p2 );

session.getTransaction().commit();

// will actually resolve to false!
assert club.getMembers().contains( p1 );