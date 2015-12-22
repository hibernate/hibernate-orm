@Entity
public class Person {

    @Id
    private Long id;

    public Person() {}

    public Person(Long id) {
        this.id = id;
    }

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name="type")
    @MapKeyEnumerated
    private Map<PhoneType, Phone> phoneRegister = new HashMap<>();

    public Map<PhoneType, Phone> getPhoneRegister() {
        return phoneRegister;
    }

    public void addPhone(Phone phone) {
        phone.setPerson(this);
        phoneRegister.put(phone.getType(), phone);
    }
}

@Entity
public class Phone {

    @Id
    @GeneratedValue
    private Long id;

    private PhoneType type;

    private String number;

    private Date since;

    @ManyToOne
    private Person person;

    public Phone() {}

    public Phone(PhoneType type, String number, Date since) {
        this.type = type;
        this.number = number;
        this.since = since;
    }

    public PhoneType getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public Date getSince() {
        return since;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}