@Entity
public class User {
	@Id
	@GeneratedValue
	Long id;

	@NaturalId
	String userName;

	...
}

// use getReference() to create associations...
Resource aResource = (Resource) session.byId( Resource.class ).getReference( 123 );
User aUser = (User) session.bySimpleNaturalId( User.class ).getReference( "steve" );
aResource.assignTo( user );


// use load() to pull initialzed data
return session.bySimpleNaturalId( User.class ).load( "steve" );