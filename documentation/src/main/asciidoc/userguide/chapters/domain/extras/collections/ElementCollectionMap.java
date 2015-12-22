@Entity
public class Person {

    @Id
    private Long id;

    public Person() {
    }

    public Person(Long id) {
        this.id = id;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @ElementCollection
    @CollectionTable(name="phone_register")
    @Column(name="since")
    @MapKeyJoinColumn(name = "phone_id", referencedColumnName="id")
    private Map<Phone, Date> phoneRegister = new HashMap<>();

    public Map<Phone, Date> getPhoneRegister() {
        return phoneRegister;
    }
}

public enum PhoneType {
    LAND_LINE,
    MOBILE
}

@Embeddable
public class Phone  {

    private PhoneType type;

    private String number;

    public Phone() {
    }

    public Phone(PhoneType type, String number) {
        this.type = type;
        this.number = number;
    }

    public PhoneType getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }
}