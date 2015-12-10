@Entity
public class Simple {

    private Integer id;

    @Id
    public Integer getId() {
        return id;
    }

    public void setId( Integer id ) {
        this.id = id;
    }
}