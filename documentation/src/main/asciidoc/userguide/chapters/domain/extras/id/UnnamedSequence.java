@Entity
public class MyEntity {

    @Id
    @GeneratedValue( generation = SEQUENCE )
    public Integer id;

    ...
}