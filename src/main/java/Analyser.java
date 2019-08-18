import api.APIClass;
import api.APIDatabase;
import api.RelClass;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import git.GitOperator;
import javafx.util.Pair;
import model.ApiObject;
import model.ClassObject;
import model.FileObject;
import model.MethodObject;
import neo4j.GitRelationships;
import neo4j.Neo4jFuncs;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.neo4j.graphdb.Node;

import java.io.*;
import java.sql.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Carol on 2018/11/27.
 */
public class Analyser {
    private String project;
    private String filter;
    private GitOperator gitOperator;
    private List<Ref> branches;
    private List<Ref> tags;
    private Neo4jFuncs neo4j;

    public Analyser(String project, String filter){
        this.project = project;
        gitOperator = new GitOperator(project, filter);
        neo4j = new Neo4jFuncs();
    }

    public void run() throws FileNotFoundException {
        APIDatabase apiDatabase = new APIDatabase();
//        InputStream api_file1 = this.getClass().getResourceAsStream("current.txt");
        //apiDatabase.load_api(api_file1);
        //InputStream api_file2 = this.getClass().getResourceAsStream("system-current.txt");
        //apiDatabase.load_api(api_file2);
        //if (true) return;
        tags = gitOperator.getTags() ;
        System.out.println(tags.size());
        Node tag_node, api_node, last_tag_node = null;
        List<ApiObject> allApis = new ArrayList<>();
        Map<Integer, RelClass> relationships = new HashMap<>();
        int count = 1;
        for (Ref tag : tags) {
            System.out.println(tag.getName() + ": " + count + "/" + tags.size() + " " + new Date().toString());
            count = count + 1;
            if (count > 113) break;
            //gitOperator.revertToTag(tag);
            if(tag.getName().equals("refs/heads/master")){
                // Do hard reset here
                continue;
            }
            tag_node = neo4j.matchTag(tag.getName());
            Boolean newTag = false;
            if (tag_node == null) {
                tag_node = neo4j.createTagNode(tag.getName());
                newTag = true;
            }
            if (last_tag_node != null) {
                neo4j.createRelationship(tag_node, last_tag_node, GitRelationships.TagToTag);
            }
            last_tag_node = tag_node;
            List<FileObject> files = gitOperator.getCommitFiles(tag);
            ArrayList<String> introduce = new ArrayList<String>();
            ArrayList<String> remove = new ArrayList<String>();
            introduce.add("api/current.txt");
            remove.add("api/removed.txt");
            introduce.add("api/system-current.txt");
            remove.add("api/system-removed.txt");
            for(FileObject fileObject : files) {
                Map<String, APIClass> apimap = new HashMap<>();
                System.out.println(fileObject.getPath());
                String content = fileObject.getFiledata();
                String[] lines = content.split("\n");
                String package_name = null;
                String class_name;
                String class_path;
                String father_class = null;
                String method_name;
                APIClass apiClass = null;
                for (String line : lines){
                    if (line.contains("package") && line.contains("{")){
                        Pattern p = Pattern.compile("package\\s+(\\S+)\\s+\\{");
                        Matcher m = p.matcher(line);
                        if(m.find())
                            package_name = m.group(1);
                    }

                    else if (line.contains("class") && line.contains("{")){
                        Pattern p = Pattern.compile("class\\s+(\\S+)\\s+");
                        Matcher m = p.matcher(line);
                        if(m.find()){
                            if (apiClass != null){
                                apimap.put(apiClass.getName(), apiClass);
                            }
                            apiClass = new APIClass();
                            class_name = m.group(1);
                            class_path = package_name + "." + class_name;
                            apiClass.setName(class_path);
                            if (line.contains("extends")){
                                p = Pattern.compile("extends\\s+(\\S+)\\s+");
                                m = p.matcher(line);
                                if(m.find()) {
                                    father_class = m.group(1);
                                    apiClass.setFather(father_class);
                                }
                            }
                        }

                    }
                    else if (line.contains("method ") && line.contains("(")){
                        Pattern p = Pattern.compile("\\s+(\\w+\\(.*\\))");
                        Matcher m = p.matcher(line);
                        if(m.find()) {
                            method_name = m.group(1);
                            apiClass.addMethod(method_name);
                            ApiObject ao = new ApiObject();
                            ao.setName(method_name);
                            ao.setPackage_name(apiClass.name);
                            int idxApi = allApis.indexOf(ao);
                            Boolean newApi = false;
                            if (idxApi == -1) {
                                //api_node = neo4j.createAPINode(ao);
                                allApis.add(ao);
                                idxApi = allApis.size()-1;
                                newApi = true;
                            }
                            RelClass rela = relationships.get(idxApi);
                            if (rela == null) {
                                relationships.put(idxApi, new RelClass());
                                rela = relationships.get(idxApi);
                            }
                            if (introduce.contains(fileObject.getPath())) {
                                //neo4j.createRelationship(tag_node, api_node, GitRelationships.Introduce);
                                rela.addRel(new Pair<>(tag_node.getId(), GitRelationships.Introduce));
                            } else if (remove.contains(fileObject.getPath())) {
                                //neo4j.createRelationship(tag_node, api_node, GitRelationships.Remove);
                                rela.addRel(new Pair<>(tag_node.getId(), GitRelationships.Remove));
                            }
                        }
                    }
                }
           }
        }
        int maxRel = 0;
        for (Map.Entry api : relationships.entrySet()) {
            RelClass rela = (RelClass) api.getValue();
            maxRel += rela.getRels().size();
        }
        for (Map.Entry api : relationships.entrySet()) {
            System.out.println((int)api.getKey() + "/" + relationships.size());
            ApiObject ao = allApis.get((int)api.getKey());
            Node apiNode = neo4j.createAPINode(ao);
            RelClass rela = (RelClass) api.getValue();
            for (Pair<Long, GitRelationships> r : rela.getRels()){
                Node tagNode = neo4j.getTagById(r.getKey());
                GitRelationships gitRel = r.getValue();
                neo4j.createRelationship(tagNode, apiNode, gitRel);
            }
        }
        System.out.println(maxRel);
        neo4j.db.shutdown();
    }
}
