@Entity
public class PersonDetails  {

    @Id
    private Long id;

    private String nickName;

    @ManyToOne
    @PrimaryKeyJoinColumn
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
        this.id = person.getId();
    }
}