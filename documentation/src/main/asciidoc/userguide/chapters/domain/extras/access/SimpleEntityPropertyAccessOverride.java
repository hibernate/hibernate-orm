@Entity
public class Simple {

    private Integer id;

    @Version
    @Access( AccessType.FIELD )
    private Integer version;

    @Id
    public Integer getId() {
        return id;
    }

    public void setId( Integer id ) {
        this.id = id;
    }
}