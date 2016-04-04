@Entity
@IdClass( PK.class )
public class Login {

    @Id
    @ManyToOne
    private System system;

    @Id
    private String username;

    public static class PK implements Serializable {

        private System system;

        private String username;

        ...
    }

    ...
}