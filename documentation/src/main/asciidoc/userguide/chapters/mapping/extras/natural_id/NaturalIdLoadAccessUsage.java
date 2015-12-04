Session session = ...;

Company company = session.byNaturalId( Company.class )
		.using( "taxIdentifier", "abc-123-xyz" )
		.load();

PostalCarrier carrier = session.byNaturalId( PostalCarrier.class )
		.using( "postalCode", new PostalCode( ... ) )
		.load();

Department department = ...;
Course course = session.byNaturalId( Course.class )
		.using( "department", department )
		.using( "code", "101" )
		.load();