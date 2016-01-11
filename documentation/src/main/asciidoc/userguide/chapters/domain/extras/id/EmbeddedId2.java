@Entity
public class Login {

    @EmbeddedId
    private PK pk;

    @Embeddable
    public static class PK implements Serializable {

        @ManyToOne
        private System system;

        private String username;

        ...
    }

    ...
}