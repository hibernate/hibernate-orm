@Entity
public class Person  {

    @Id
    private Long id;

    @ElementCollection
    private List<Phone> phones = new ArrayList<>();

    public List<Phone> getPhones() {
        return phones;
    }
}

@Embeddable
public class Phone  {

    private String type;

    private String number;

    public Phone() {}

    public Phone(String type, String number) {
        this.type = type;
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }
}

person.getPhones().add( new Phone( "landline", "028-234-9876" ) );
person.getPhones().add( new Phone( "mobile", "072-122-9876" ) );