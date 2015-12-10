@Entity
public class Patch {

    @Id
    private Long id;

    @ElementCollection
    @CollectionTable(
        name="patch_change",
        joinColumns=@JoinColumn(name="patch_id")
    )
    @OrderColumn(name = "index_id")
    private List<Change> changes = new ArrayList<>();

    public List<Change> getChanges() {
        return changes;
    }
}