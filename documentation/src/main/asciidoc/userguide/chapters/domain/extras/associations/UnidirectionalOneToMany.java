@Entity
public class Person  {

    @Id
    @GeneratedValue
    private Long id;

    public Person() {}

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Phone> phones = new ArrayList<>();

    public List<Phone> getPhones() {
        return phones;
    }
}

@Entity
public class Phone  {

    @Id
    @GeneratedValue
    private Long id;

    private String number;

    public Phone() {}

    public Phone(String number) {
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }
}