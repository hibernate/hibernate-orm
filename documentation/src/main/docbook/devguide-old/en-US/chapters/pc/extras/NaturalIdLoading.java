import java.lang.String;

@Entity
public class User {
	@Id
	@GeneratedValue
	Long id;

	@NaturalId
	String system;

	@NaturalId
	String userName;

	...
}

// use getReference() to create associations...
Resource aResource = (Resource) session.byId( Resource.class ).getReference( 123 );
User aUser = (User) session.byNaturalId( User.class )
		.using( "system", "prod" )
		.using( "userName", "steve" )
		.getReference();
aResource.assignTo( user );


// use load() to pull initialzed data
return session.byNaturalId( User.class )
		.using( "system", "prod" )
		.using( "userName", "steve" )
		.load();