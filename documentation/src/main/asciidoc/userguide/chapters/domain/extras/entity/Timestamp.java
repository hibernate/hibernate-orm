@Entity
public class Thing {

    @Id
    private Integer id;

    @Version
    private Timestamp ts;

    ...
}