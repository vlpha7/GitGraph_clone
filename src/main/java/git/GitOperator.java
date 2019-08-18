package git;

import com.google.common.collect.Lists;
import helper.TypeFilter;
import model.FileObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Carol on 2018/11/27.
 */

public class GitOperator {
    private String gitProject;
    private Repository repository;
    private Git git;
    private TreeWalk treeWalk;
    private TypeFilter typeFilter = null;

    public GitOperator(String gitProject, String filter){
        this.gitProject = gitProject;
        this.typeFilter = new TypeFilter();
        if(filter.contains("p"))
            this.typeFilter.setPicFilter(true);
        if(filter.contains("v"))
            this.typeFilter.setVideoFilter(true);
        if(filter.contains("a"))
            this.typeFilter.setAudioFilter(true);
        if(filter.contains("c"))
            this.typeFilter.setCustomFilter(true);
        try {
            repository = FileRepositoryBuilder.create(new File(gitProject, ".git"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        git = new Git(repository);
        treeWalk = new TreeWalk(repository );
    }

    public List<Ref> getTags() {
        try {
            return git.tagList().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Ref> getBranches() {
        try {
            return git.branchList().call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<RevCommit> getBranchCommits(Ref branch){
        String branchName = branch.getName();
        Iterable<RevCommit> commits = null;
        try {
            commits = git.log().add(repository.resolve(branchName)).call();
            return Lists.newArrayList(commits.iterator());
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void revertToTag(Ref tag) {
        try {
            git.reset().addPath("api").setRef(tag.getName()).call();
            System.out.println("done reset to " + tag.getName());
            return;
        } catch (CheckoutConflictException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return;
    }

    public List<FileObject> getCommitFiles(Ref tag){
        try{
            RevWalk revWalk = new RevWalk(repository);
            RevCommit commit = revWalk.parseCommit(tag.getPeeledObjectId());
            treeWalk = new TreeWalk(repository );
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            ArrayList<FileObject> result = new ArrayList<FileObject>();
            ArrayList<String> introduce = new ArrayList<String>();
            ArrayList<String> remove = new ArrayList<String>();
            introduce.add("api/current.txt");
            remove.add("api/removed.txt");
            introduce.add("api/system-current.txt");
            remove.add("api/system-removed.txt");
            // TODO: old tag such as refs/tags/android-2.2.3_r2 contain current.xml instead of current.txt
            while(treeWalk.next()) {
                try {
                    String filename = treeWalk.getNameString();
                    String path = treeWalk.getPathString();
                    if (!remove.contains(path) && !introduce.contains(path)) {
                        continue;
                    }
                    ObjectId objectid = treeWalk.getObjectId(0);
                    FileObject fileObject = new FileObject(path, filename, objectid);
                    if (typeFilter.isFilter(fileObject.getType()))
                        continue;

                    ObjectLoader loader = repository.open(objectid);
                    fileObject.setFiledata(new String(loader.getBytes(), "UTF-8"));
                    result.add(fileObject);
                }catch (MissingObjectException e){
                    System.out.println(e);
                }
            }
            return result;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<FileObject> getCommitFiles(RevCommit commit){
        try{
            treeWalk = new TreeWalk(repository );
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            ArrayList<FileObject> result = new ArrayList<FileObject>();
            while(treeWalk.next()) {
                try {
                    String filename = treeWalk.getNameString();
                    String path = treeWalk.getPathString();
                    ObjectId objectid = treeWalk.getObjectId(0);
                    FileObject fileObject = new FileObject(path, filename, objectid);
                    if (typeFilter.isFilter(fileObject.getType()))
                        continue;

                    ObjectLoader loader = repository.open(objectid);
                    fileObject.setFiledata(new String(loader.getBytes(), "UTF-8"));
                    result.add(fileObject);
                }catch (MissingObjectException e){
                    System.out.println(e);
                }
            }
            return result;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
