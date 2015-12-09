@Entity
public class Login {

    @EmbeddedId
    private PK pk;

    @Embeddable
    public static class PK implements Serializable {

        private String system;

        private String username;

        ...
    }

    ...
}