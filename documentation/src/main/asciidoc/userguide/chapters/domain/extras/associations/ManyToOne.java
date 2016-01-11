@Entity
public class Person  {

    @Id
    @GeneratedValue
    private Long id;

    public Person() {}
}

@Entity
public class Phone  {

    @Id
    @GeneratedValue
    private Long id;

    private String number;

    @ManyToOne
    @JoinColumn(name = "person_id",
        foreignKey = @ForeignKey(name = "PERSON_ID_FK")
    )
    private Person person;

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

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}