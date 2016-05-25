@Entity
public class Person {

    @Id
    @GeneratedValue
    private Integer id;

    @NaturalId
    private String ssn;

    protected Person() {
        // Constructor for ORM
    }

    public Person( String ssn ) {
        // Constructor for app
        this.ssn = ssn;
    }

    @Override
    public int hashCode() {
        return Objects.hash( ssn );
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof Person ) ) {
            return false;
        }
        Person person = (Person) o;
        return Objects.equals( ssn, person.ssn );
    }
}