Session session = ...;

Person person = session.bySimpleNaturalId( Person.class )
		.load( "123-45-6789" );
person.setSsn( "987-65-4321" );

...

// returns null!
person = session.bySimpleNaturalId( Person.class )
		.setSynchronizationEnabled( false )
		.load( "987-65-4321" );

// returns correctly!
person = session.bySimpleNaturalId( Person.class )
		.setSynchronizationEnabled( true )
		.load( "987-65-4321" );