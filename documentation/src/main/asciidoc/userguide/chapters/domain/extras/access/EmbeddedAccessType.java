@Entity
public class Patch {

    @Id
    private Long id;

    @Embedded
    private Change change;
}