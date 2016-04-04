@Entity
public class Simple {

    @Id
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId( Integer id ) {
        this.id = id;
    }
}