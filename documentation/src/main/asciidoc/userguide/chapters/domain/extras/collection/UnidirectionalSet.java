@Entity
public class Person {

    @Id
    private Long id;

    public Person() {
    }

    public Person(Long id) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL)
    private Set<Phone> phones = new HashSet<>();

    public Set<Phone> getPhones() {
        return phones;
    }
}

@Entity
public class Phone {

    @Id
    private Long id;

    private String type;

    @NaturalId
    private String number;

    public Phone() {
    }

    public Phone(Long id, String type, String number) {
        this.id = id;
        this.type = type;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phone phone = (Phone) o;
        return Objects.equals(number, phone.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }
}