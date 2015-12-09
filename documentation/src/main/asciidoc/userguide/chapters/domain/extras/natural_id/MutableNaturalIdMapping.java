@Entity
public class Person {

    @Id
    private Integer id;

    @NaturalId( mutable = true )
    private String ssn;

    ...
}