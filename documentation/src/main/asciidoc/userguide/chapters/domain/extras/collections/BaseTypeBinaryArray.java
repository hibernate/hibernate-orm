@Entity
public class Person  {

    @Id
    private Long id;

    public Person() {}

    public Person(Long id) {
        this.id = id;
    }

    private String[] phones;

    public String[] getPhones() {
        return phones;
    }

    public void setPhones(String[] phones) {
        this.phones = phones;
    }
}