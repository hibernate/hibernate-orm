@Entity
@IdClass( PK.class )
public class LogFile {

    @Id
    private String name;

    @Id
    private LocalDate date;

    @Id
    @GeneratedValue
    private Integer uniqueStamp;

    public static class PK implements Serializable {

        private String name;

        private LocalDate date;

        private Integer uniqueStamp;

        ...
    }

    ...
}

