@Entity
public class MyEntity {

    @Id
    @GeneratedValue( generation = TABLE )
    public Integer id;

    ...
}