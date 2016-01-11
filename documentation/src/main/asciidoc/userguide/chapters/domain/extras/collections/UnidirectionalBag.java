@Entity
public class Person  {

    @Id
    private Long id;

    public Person() {}

    public Person( Long id ) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL)
    private List<Phone> phones = new ArrayList<>();

    public List<Phone> getPhones() {
        return phones;
    }
}

@Entity
public class Phone  {

    @Id
    private Long id;

    private String type;

    private String number;

    public Phone() {
    }

    public Phone( Long id, String type, String number ) {
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
}