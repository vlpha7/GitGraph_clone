package api;

import javafx.util.Pair;
import neo4j.GitRelationships;

import java.util.ArrayList;
import java.util.List;

public class RelClass {
    public List<Pair<Long, GitRelationships>> allRels = new ArrayList<>();
    public List<Pair<Long, GitRelationships>> getRels() {
        return allRels;
    }
    public void addRel(Pair<Long, GitRelationships> relationship) {
        this.allRels.add(relationship);
    }
}
