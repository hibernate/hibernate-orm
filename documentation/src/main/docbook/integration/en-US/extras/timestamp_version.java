@Entity
public class Flight implements Serializable {
...
    @Version
    public Date getLastUpdate() { ... }
} 