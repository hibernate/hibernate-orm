@Entity
public class Person {

    @Id
    @GeneratedValue
    private Integer id;

    @Override
    public int hashCode() {
        return Objects.hash( id );
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
        return Objects.equals( id, person.id );
    }
}