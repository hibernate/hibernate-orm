Session session = ...;

Company company = session.bySimpleNaturalId( Company.class )
		.load( "abc-123-xyz" );

PostalCarrier carrier = session.bySimpleNaturalId( PostalCarrier.class )
		.load( new PostalCode( ... ) );