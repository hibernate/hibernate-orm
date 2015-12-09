@Entity
public class PostalCarrier {

    @Id
    private Integer id;

    @NaturalId
    @Embedded
    private PostalCode postalCode;

    ...

}

@Embeddable
public class PostalCode {
    ...
}
