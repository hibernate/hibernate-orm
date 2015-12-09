@Entity
public class Contact {

    @Id
    private Integer id;

    @Embedded
    private Name name;

    @Embedded
    private Address homeAddress;

    @Embedded
    private Address mailingAddress;

    @Embedded
    private Address workAddress;

    ...
}