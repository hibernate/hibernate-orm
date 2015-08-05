String userid = ...;
session.enableFetchProfile( "employee.projects" );
Employee e = (Employee) session.bySimpleNaturalId( Employee.class )
		.load( userid );