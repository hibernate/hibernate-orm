@Entity
public class Person {

    @Id
    private Long id;

    public Person() {
    }

    public Person(Long id) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
            name="phone_register",
            joinColumns = @JoinColumn(name = "phone_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id"))
    @MapKey(name="since")
    @MapKeyTemporal(TemporalType.TIMESTAMP)
    private Map<Date, Phone> phoneRegister = new HashMap<>();

    public Map<Date, Phone> getPhoneRegister() {
        return phoneRegister;
    }

    public void addPhone(Phone phone) {
        phoneRegister.put(phone.getSince(), phone);
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
}