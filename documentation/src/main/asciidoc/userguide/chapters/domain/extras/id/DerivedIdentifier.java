@Entity
public class Person  {

    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    private String registrationNumber;

    public Person() {}

    public Person(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Long getId() {
        return id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }
}

@Entity
public class PersonDetails  {

    @Id
    private Long id;

    private String nickName;

    @ManyToOne
    @MapsId
    private Person person;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}