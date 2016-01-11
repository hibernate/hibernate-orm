@Entity
public class Person  {

    @Id
    @GeneratedValue
    private Long id;

    public Person() {}

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE} )
    private List<Address> addresses = new ArrayList<>();

    public List<Address> getAddresses() {
        return addresses;
    }
}

@Entity
public class Address  {

    @Id
    @GeneratedValue
    private Long id;

    private String street;

    private String number;

    public Address() {}

    public Address(String street, String number) {
        this.street = street;
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }
}