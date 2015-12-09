@Entity
@IdClass( PK.class )
public class Login {

    @Id
    private String system;

    @Id
    private String username;

    public static class PK implements Serializable {

        private String system;

        private String username;

        ...
    }

    ...
}